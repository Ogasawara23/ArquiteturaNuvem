package br.com.smartkds.pedido.features.registrarentregadopedido;

import br.com.smartkds.pedido.shared.dominio.TenantId;

import java.util.List;
import java.util.UUID;

record RegistrarEntregaCommand(UUID eventoId, TenantId tenantId, UUID comandaId, List<UUID> itemComandaIds) {
}
