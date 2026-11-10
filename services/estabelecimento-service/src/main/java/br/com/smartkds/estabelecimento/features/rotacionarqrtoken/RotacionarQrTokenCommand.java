package br.com.smartkds.estabelecimento.features.rotacionarqrtoken;

import br.com.smartkds.estabelecimento.shared.dominio.TenantId;

import java.util.UUID;

record RotacionarQrTokenCommand(TenantId tenantId, UUID mesaId) {
}
