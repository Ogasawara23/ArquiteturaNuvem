package br.com.smartkds.producao.features.avancarstatusticket;

import br.com.smartkds.producao.shared.dominio.StatusTicket;
import br.com.smartkds.producao.shared.dominio.TenantId;

import java.util.UUID;

record AvancarStatusTicketCommand(TenantId tenantId, UUID ticketId, StatusTicket novoStatus) {
}
