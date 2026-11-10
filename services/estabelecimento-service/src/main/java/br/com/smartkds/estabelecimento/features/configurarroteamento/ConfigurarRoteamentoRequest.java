package br.com.smartkds.estabelecimento.features.configurarroteamento;

import java.util.List;

record ConfigurarRoteamentoRequest(String estacaoPadrao, List<RegraDto> regras) {

    record RegraDto(String itemCardapioId, String categoria, String estacaoId, Integer prioridade) {
    }
}
