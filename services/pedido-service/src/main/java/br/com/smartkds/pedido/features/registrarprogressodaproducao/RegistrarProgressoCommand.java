package br.com.smartkds.pedido.features.registrarprogressodaproducao;

import br.com.smartkds.pedido.shared.dominio.TenantId;

import java.util.List;
import java.util.UUID;

record RegistrarProgressoCommand(UUID eventoId, TenantId tenantId, UUID comandaId, String statusDoTicket,
                                 List<UUID> itemComandaIds) {
}
