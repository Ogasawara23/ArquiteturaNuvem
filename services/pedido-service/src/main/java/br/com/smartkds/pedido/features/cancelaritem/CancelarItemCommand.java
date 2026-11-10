package br.com.smartkds.pedido.features.cancelaritem;

import br.com.smartkds.pedido.shared.dominio.TenantId;

import java.util.UUID;

record CancelarItemCommand(TenantId tenantId, UUID comandaId, UUID itemId, String motivo,
                           String usuarioId, String correlationId) {
}
