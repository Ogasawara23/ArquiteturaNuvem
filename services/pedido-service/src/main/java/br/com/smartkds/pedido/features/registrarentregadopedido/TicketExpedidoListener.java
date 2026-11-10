package br.com.smartkds.pedido.features.registrarentregadopedido;

import br.com.smartkds.pedido.shared.dominio.TenantId;
import br.com.smartkds.pedido.shared.tenancy.ContextoDoTenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Ponto de entrada AMQP da fatia: desserializa, restaura o contexto e delega ao Handler. */
@Component
class TicketExpedidoListener {

    private final RegistrarEntregaHandler handler;
    private final ObjectMapper json;

    TicketExpedidoListener(RegistrarEntregaHandler handler, ObjectMapper json) {
        this.handler = handler;
        this.json = json;
    }

    @RabbitListener(queues = "pedido.producao")
    public void aoReceber(Message mensagem) throws IOException {
        TicketExpedidoMessage evento = json.readValue(mensagem.getBody(), TicketExpedidoMessage.class);
        String traceparent = mensagem.getMessageProperties().getHeader("traceparent");
        TenantId tenant = new TenantId(evento.tenantId());
        ContextoDoTenant.executarComo(tenant, evento.eventoId().toString(), traceparent, () -> {
            handler.executar(new RegistrarEntregaCommand(evento.eventoId(), tenant, evento.comandaId(),
                    evento.itemComandaIds()));
            return null;
        });
    }
}
