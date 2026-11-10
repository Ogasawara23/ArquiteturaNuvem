package br.com.smartkds.catalogo.features.alterardisponibilidade;

import br.com.smartkds.catalogo.shared.dominio.TenantId;

record AlterarDisponibilidadeCommand(TenantId tenantId, String itemId, boolean disponivel) {
}
