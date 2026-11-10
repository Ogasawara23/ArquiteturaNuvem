package br.com.smartkds.pedido.features.cancelarcomanda;

import br.com.smartkds.pedido.shared.dominio.TenantId;

import java.util.UUID;

record CancelarComandaCommand(TenantId tenantId, UUID comandaId, String motivo, String usuarioId, String correlationId) {
}
