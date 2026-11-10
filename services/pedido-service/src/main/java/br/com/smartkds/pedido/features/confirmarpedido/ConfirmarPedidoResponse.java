package br.com.smartkds.pedido.features.confirmarpedido;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record ConfirmarPedidoResponse(UUID comandaId,
                               List<ItemConfirmadoDto> itensConfirmados,
                               Instant confirmadoEm,
                               boolean repeticao) {
}
