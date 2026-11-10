package br.com.smartkds.catalogo.features.atualizaritem;

import br.com.smartkds.catalogo.shared.dominio.DadosDoItem;
import br.com.smartkds.catalogo.shared.dominio.TenantId;

record AtualizarItemCommand(TenantId tenantId, String itemId, DadosDoItem dados) {
}
