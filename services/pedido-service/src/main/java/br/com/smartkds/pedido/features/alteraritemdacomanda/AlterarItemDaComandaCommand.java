package br.com.smartkds.pedido.features.alteraritemdacomanda;

import br.com.smartkds.pedido.shared.dominio.TenantId;

import java.util.UUID;

record AlterarItemDaComandaCommand(TenantId tenantId, UUID comandaId, UUID itemId, int quantidade, String observacao) {
}
