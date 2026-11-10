package br.com.smartkds.pedido.features.confirmarpedido;

import java.util.UUID;

record ItemConfirmadoDto(UUID itemId, String descricao, int quantidade) {
}
