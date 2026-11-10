package br.com.smartkds.pedido.features.fecharcomanda;

import br.com.smartkds.pedido.shared.dominio.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

record FecharComandaCommand(TenantId tenantId, UUID comandaId, BigDecimal percentualTaxaServico,
                            String usuarioId, String correlationId) {
}
