package br.com.smartkds.estabelecimento.features.consultarestabelecimento;

import java.math.BigDecimal;

record EstabelecimentoDto(String tenantId, String nomeFantasia, String fusoHorario, BigDecimal percentualTaxaServico,
                          String estacaoPadrao, boolean ativo, int quantidadeDeMesas, int quantidadeDeEstacoes) {
}
