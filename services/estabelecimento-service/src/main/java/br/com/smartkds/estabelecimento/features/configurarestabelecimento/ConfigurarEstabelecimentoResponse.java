package br.com.smartkds.estabelecimento.features.configurarestabelecimento;

import java.math.BigDecimal;

record ConfigurarEstabelecimentoResponse(String tenantId, String nomeFantasia, String fusoHorario,
                                         BigDecimal percentualTaxaServico, String estacaoPadrao) {
}
