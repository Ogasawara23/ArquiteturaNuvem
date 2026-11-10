package br.com.smartkds.producao.features.gerartickets;

import br.com.smartkds.producao.shared.dominio.ItemParaProduzir;
import br.com.smartkds.producao.shared.dominio.TenantId;

import java.util.List;
import java.util.UUID;

record GerarTicketsCommand(UUID eventoId, TenantId tenantId, UUID comandaId, String mesaRotulo,
                           List<ItemParaProduzir> itens) {
}
