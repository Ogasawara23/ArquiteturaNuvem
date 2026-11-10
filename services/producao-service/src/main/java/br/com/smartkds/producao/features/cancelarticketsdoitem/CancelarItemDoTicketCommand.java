package br.com.smartkds.producao.features.cancelarticketsdoitem;

import br.com.smartkds.producao.shared.dominio.TenantId;

import java.util.UUID;

record CancelarItemDoTicketCommand(UUID eventoId, TenantId tenantId, UUID itemComandaId) {
}
