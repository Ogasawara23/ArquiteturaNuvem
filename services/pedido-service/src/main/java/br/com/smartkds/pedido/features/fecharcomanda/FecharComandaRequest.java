package br.com.smartkds.pedido.features.fecharcomanda;

import java.math.BigDecimal;

record FecharComandaRequest(BigDecimal percentualTaxaServico) {
}
