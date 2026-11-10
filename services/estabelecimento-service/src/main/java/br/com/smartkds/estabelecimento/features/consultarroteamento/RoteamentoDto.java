package br.com.smartkds.estabelecimento.features.consultarroteamento;

import java.util.List;

/** Contrato consumido pelo producao-service (EstabelecimentoHttpClient). */
record RoteamentoDto(String estacaoPadrao, List<Regra> regras) {

    record Regra(String itemCardapioId, String categoria, String estacaoId, int prioridade) {
    }
}
