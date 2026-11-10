package br.com.smartkds.estabelecimento.features.cadastrarmesa;

import br.com.smartkds.estabelecimento.shared.dominio.TenantId;

record CadastrarMesaCommand(TenantId tenantId, String rotulo) {
}
