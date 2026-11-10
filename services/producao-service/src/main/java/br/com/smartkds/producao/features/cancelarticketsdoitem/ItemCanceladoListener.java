package br.com.smartkds.producao.features.cancelarticketsdoitem;

import br.com.smartkds.producao.shared.dominio.TenantId;
import br.com.smartkds.producao.shared.tenancy.ContextoDoTenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
class ItemCanceladoListener {

    private final CancelarItemDoTicketHandler handler;
    private final ObjectMapper json;

    ItemCanceladoListener(CancelarItemDoTicketHandler handler, ObjectMapper json) {
        this.handler = handler;
        this.json = json;
    }

    @RabbitListener(queues = "producao.cancelamentos")
    public void aoReceber(Message mensagem) throws IOException {
        ItemCanceladoMessage evento = json.readValue(mensagem.getBody(), ItemCanceladoMessage.class);
        TenantId tenant = new TenantId(evento.tenantId());
        ContextoDoTenant.executarComo(tenant, evento.eventoId().toString(),
                mensagem.getMessageProperties().getHeader("traceparent"), () -> {
                    handler.executar(new CancelarItemDoTicketCommand(evento.eventoId(), tenant, evento.itemComandaId()));
                    return null;
                });
    }
}
