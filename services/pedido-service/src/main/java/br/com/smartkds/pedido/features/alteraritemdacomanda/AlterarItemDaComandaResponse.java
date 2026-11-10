package br.com.smartkds.pedido.features.alteraritemdacomanda;

import java.util.UUID;

record AlterarItemDaComandaResponse(UUID itemId, int quantidade, String observacao, long totalItemCentavos,
                                    long subtotalComandaCentavos) {
}
