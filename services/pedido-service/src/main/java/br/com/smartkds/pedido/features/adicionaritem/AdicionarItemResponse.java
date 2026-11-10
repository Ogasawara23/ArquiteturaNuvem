package br.com.smartkds.pedido.features.adicionaritem;

import java.util.UUID;

record AdicionarItemResponse(UUID itemId,
                             String descricao,
                             long precoUnitarioCentavos,
                             long totalItemCentavos,
                             long subtotalComandaCentavos) {
}
