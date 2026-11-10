package br.com.smartkds.estabelecimento.features.configurarroteamento;

import br.com.smartkds.estabelecimento.shared.dominio.RegraDeRoteamento;
import br.com.smartkds.estabelecimento.shared.dominio.TenantId;

import java.util.List;

record ConfigurarRoteamentoCommand(TenantId tenantId, String estacaoPadrao, List<RegraDeRoteamento> regras) {
}
