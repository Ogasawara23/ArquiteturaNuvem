package br.com.smartkds.pedido.shared.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Relay da Transactional Outbox (Figura 15, passos 21 a 25): le eventos nao publicados,
 * publica no RabbitMQ com confirmacao do broker e marca publicado_em. Entrega ao menos uma vez.
 */
@Component
public class OutboxRelay {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);
    private static final Duration ESPERA_APOS_FALHA = Duration.ofSeconds(5);

    private final OutboxSpringData outbox;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate transacao;
    private final Clock relogio;
    private final String exchange;
    private final int lote;
    private volatile Instant proximaTentativa = Instant.EPOCH;

    @SuppressWarnings("java:S107")
    OutboxRelay(OutboxSpringData outbox, RabbitTemplate rabbit, TransactionTemplate transacao, Clock relogio,
                MeterRegistry metricas,
                @Value("${smartkds.outbox.exchange:smartkds.pedidos}") String exchange,
                @Value("${smartkds.outbox.lote:50}") int lote) {
        this.outbox = outbox;
        this.rabbit = rabbit;
        this.transacao = transacao;
        this.relogio = relogio;
        this.exchange = exchange;
        this.lote = lote;
        Gauge.builder("outbox_eventos_pendentes", outbox, OutboxSpringData::contarPendentes)
                .description("Eventos gravados e ainda nao publicados no broker")
                .register(metricas);
    }

    @Scheduled(fixedDelayString = "${smartkds.outbox.intervalo-ms:200}")
    public void publicarPendentes() {
        if (Instant.now(relogio).isBefore(proximaTentativa)) {
            return;
        }
        try {
            transacao.executeWithoutResult(status -> {
                for (RegistroOutboxEntity registro : outbox.travarPendentes(org.springframework.data.domain.PageRequest.of(0, lote))) {
                    try {
                        enviar(registro);
                        registro.publicadoEm = Instant.now(relogio);
                    } catch (RuntimeException e) {
                        registro.tentativas++;
                        proximaTentativa = Instant.now(relogio).plus(ESPERA_APOS_FALHA);
                        LOG.warn("falha ao publicar evento {} (tentativa {}): {}",
                                registro.eventoId, registro.tentativas, e.getMessage());
                        break;
                    }
                }
            });
        } catch (RuntimeException e) {
            proximaTentativa = Instant.now(relogio).plus(ESPERA_APOS_FALHA);
            LOG.warn("relay da outbox indisponivel: {}", e.getMessage());
        }
    }

    private void enviar(RegistroOutboxEntity registro) {
        MessageProperties propriedades = new MessageProperties();
        propriedades.setMessageId(registro.eventoId.toString());
        propriedades.setType(registro.tipo);
        propriedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propriedades.setContentEncoding(StandardCharsets.UTF_8.name());
        propriedades.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        propriedades.setHeader("tenant_id", registro.tenantId);
        if (registro.traceparent != null) {
            propriedades.setHeader("traceparent", registro.traceparent);
        }
        var mensagem = MessageBuilder.withBody(registro.payload.getBytes(StandardCharsets.UTF_8))
                .andProperties(propriedades).build();
        rabbit.invoke(operacoes -> {
            operacoes.send(exchange, registro.routingKey, mensagem);
            operacoes.waitForConfirmsOrDie(5_000);
            return null;
        });
    }
}
