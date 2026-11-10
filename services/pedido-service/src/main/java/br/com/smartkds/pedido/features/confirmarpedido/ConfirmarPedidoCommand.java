package br.com.smartkds.pedido.features.confirmarpedido;

import br.com.smartkds.pedido.shared.dominio.TenantId;

import java.util.UUID;

record ConfirmarPedidoCommand(TenantId tenantId, UUID comandaId, String chaveIdempotencia) {
}
