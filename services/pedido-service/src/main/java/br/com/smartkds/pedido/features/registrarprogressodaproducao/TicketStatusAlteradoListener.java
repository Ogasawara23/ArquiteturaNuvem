package br.com.smartkds.pedido.features.registrarprogressodaproducao;

import br.com.smartkds.pedido.shared.dominio.TenantId;
import br.com.smartkds.pedido.shared.tenancy.ContextoDoTenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
class TicketStatusAlteradoListener {

    private final RegistrarProgressoHandler handler;
    private final ObjectMapper json;

    TicketStatusAlteradoListener(RegistrarProgressoHandler handler, ObjectMapper json) {
        this.handler = handler;
        this.json = json;
    }

    @RabbitListener(queues = "pedido.producao.status")
    public void aoReceber(Message mensagem) throws IOException {
        TicketStatusAlteradoMessage evento = json.readValue(mensagem.getBody(), TicketStatusAlteradoMessage.class);
        String traceparent = mensagem.getMessageProperties().getHeader("traceparent");
        TenantId tenant = new TenantId(evento.tenantId());
        ContextoDoTenant.executarComo(tenant, evento.eventoId().toString(), traceparent, () -> {
            handler.executar(new RegistrarProgressoCommand(evento.eventoId(), tenant, evento.comandaId(),
                    evento.status(), evento.itemComandaIds()));
            return null;
        });
    }
}
