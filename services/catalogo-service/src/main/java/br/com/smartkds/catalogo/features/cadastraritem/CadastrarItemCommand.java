package br.com.smartkds.catalogo.features.cadastraritem;

import br.com.smartkds.catalogo.shared.dominio.DadosDoItem;
import br.com.smartkds.catalogo.shared.dominio.TenantId;

record CadastrarItemCommand(TenantId tenantId, DadosDoItem dados) {
}
