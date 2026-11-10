package br.com.smartkds.catalogo.features.removeritem;

import br.com.smartkds.catalogo.shared.dominio.TenantId;

record RemoverItemCommand(TenantId tenantId, String itemId) {
}
