package br.com.smartkds.producao.features.gerartickets;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Contrato consumido de smartkds.pedidos / pedido.confirmado (versaoDoContrato 1). */
record PedidoConfirmadoMessage(UUID eventoId,
                               int versaoDoContrato,
                               String tenantId,
                               UUID comandaId,
                               String mesaId,
                               String mesaRotulo,
                               List<ItemPedidoMessage> itens,
                               Instant ocorridoEm) {

    record ItemPedidoMessage(UUID itemComandaId,
                             String itemCardapioId,
                             String categoria,
                             String descricao,
                             int quantidade,
                             List<String> modificadores,
                             String observacao,
                             String estacaoSugerida,
                             int tempoAlvoPreparoSegundos) {
    }
}
