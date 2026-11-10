package br.com.smartkds.producao.features.expedirticket;

import br.com.smartkds.producao.shared.dominio.TenantId;

import java.util.UUID;

record ExpedirTicketCommand(TenantId tenantId, UUID ticketId) {
}
