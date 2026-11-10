package br.com.smartkds.estabelecimento.features.cadastrarestacao;

import br.com.smartkds.estabelecimento.shared.dominio.TenantId;

record CadastrarEstacaoCommand(TenantId tenantId, String codigo, String nome, int ordemExibicao, int percentualAtencao) {
}
