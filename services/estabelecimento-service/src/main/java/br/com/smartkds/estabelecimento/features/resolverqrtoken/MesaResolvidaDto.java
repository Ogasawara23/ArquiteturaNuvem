package br.com.smartkds.estabelecimento.features.resolverqrtoken;

import java.math.BigDecimal;
import java.util.UUID;

record MesaResolvidaDto(String tenantId, UUID mesaId, String rotulo, String nomeEstabelecimento,
                        BigDecimal percentualTaxaServico) {
}
