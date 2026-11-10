package br.com.smartkds.pedido.features.fecharcomanda;

import java.time.Instant;
import java.util.UUID;

record FecharComandaResponse(UUID comandaId,
                             String mesaRotulo,
                             long subtotalCentavos,
                             long taxaServicoCentavos,
                             long totalCentavos,
                             int quantidadeDeItens,
                             Instant fechadaEm) {
}
