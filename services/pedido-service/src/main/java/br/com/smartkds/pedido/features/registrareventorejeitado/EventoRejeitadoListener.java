package br.com.smartkds.pedido.features.registrareventorejeitado;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Consumidor das dead letter queues do servico (Figura 21). */
@Component
class EventoRejeitadoListener {

    private final RegistrarEventoRejeitadoHandler handler;

    EventoRejeitadoListener(RegistrarEventoRejeitadoHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = {"pedido.producao.dlq", "pedido.producao.status.dlq"})
    public void aoReceber(Message mensagem, @Header(AmqpHeaders.CONSUMER_QUEUE) String fila) {
        var propriedades = mensagem.getMessageProperties();
        String corpo = new String(mensagem.getBody(), StandardCharsets.UTF_8);
        handler.executar(new RegistrarEventoRejeitadoCommand(propriedades.getMessageId(), propriedades.getType(),
                fila, propriedades.getHeader("traceparent"),
                corpo.length() > 500 ? corpo.substring(0, 500) : corpo));
    }
}
