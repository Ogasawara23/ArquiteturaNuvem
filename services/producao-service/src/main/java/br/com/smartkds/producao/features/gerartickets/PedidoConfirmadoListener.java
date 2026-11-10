package br.com.smartkds.producao.features.gerartickets;

import br.com.smartkds.producao.shared.dominio.ItemParaProduzir;
import br.com.smartkds.producao.shared.dominio.TenantId;
import br.com.smartkds.producao.shared.tenancy.ContextoDoTenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Figura 16, passos 1 a 3: desserializa, restaura o traceparent e delega ao handler. */
@Component
class PedidoConfirmadoListener {

    private final GerarTicketsHandler handler;
    private final ObjectMapper json;

    PedidoConfirmadoListener(GerarTicketsHandler handler, ObjectMapper json) {
        this.handler = handler;
        this.json = json;
    }

    @RabbitListener(queues = "producao.pedidos")
    public void aoReceber(Message mensagem) throws IOException {
        PedidoConfirmadoMessage evento = json.readValue(mensagem.getBody(), PedidoConfirmadoMessage.class);
        TenantId tenant = new TenantId(evento.tenantId());
        var itens = evento.itens().stream().map(i -> new ItemParaProduzir(i.itemComandaId(), i.itemCardapioId(),
                i.categoria(), i.descricao(), i.quantidade(), i.modificadores(), i.observacao(), i.estacaoSugerida(),
                i.tempoAlvoPreparoSegundos())).toList();
        ContextoDoTenant.executarComo(tenant, evento.eventoId().toString(),
                mensagem.getMessageProperties().getHeader("traceparent"), () -> {
                    handler.executar(new GerarTicketsCommand(evento.eventoId(), tenant, evento.comandaId(),
                            evento.mesaRotulo(), itens));
                    return null;
                });
    }
}
