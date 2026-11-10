package br.com.smartkds.estabelecimento.features.configurarestabelecimento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

record ConfigurarEstabelecimentoRequest(@NotBlank String nomeFantasia,
                                        @NotBlank String fusoHorario,
                                        @NotNull BigDecimal percentualTaxaServico,
                                        String estacaoPadrao) {
}
