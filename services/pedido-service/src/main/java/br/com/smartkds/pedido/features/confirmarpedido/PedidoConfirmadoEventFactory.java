package br.com.smartkds.pedido.features.confirmarpedido;

import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.dominio.ItemComanda;
import br.com.smartkds.pedido.shared.eventos.ItemConfirmadoPayload;
import br.com.smartkds.pedido.shared.eventos.PedidoConfirmado;

import java.time.Instant;
import java.util.List;

/** O evento carrega tudo o que a producao precisa: ela nunca consulta o pedido nem o catalogo. */
final class PedidoConfirmadoEventFactory {

    private PedidoConfirmadoEventFactory() { }

    static PedidoConfirmado de(Comanda comanda, List<ItemComanda> itens, String chaveIdempotencia, Instant agora) {
        List<ItemConfirmadoPayload> payload = itens.stream()
                .map(item -> new ItemConfirmadoPayload(
                        item.id().valor(),
                        item.itemCardapioId(),
                        item.categoria(),
                        item.descricao(),
                        item.quantidade(),
                        item.modificadoresComoTexto(),
                        item.observacao(),
                        item.estacaoSugerida(),
                        item.tempoAlvoPreparoSegundos()))
                .toList();

        return PedidoConfirmado.novo(
                comanda.tenantId().valor(),
                comanda.id().valor(),
                comanda.mesaId().valor(),
                comanda.mesaRotulo(),
                chaveIdempotencia,
                comanda.versaoCardapioId(),
                agora,
                payload);
    }
}
