package br.com.smartkds.catalogo.features.publicarversaocardapio;

import br.com.smartkds.catalogo.shared.dominio.TenantId;

record PublicarVersaoCardapioCommand(TenantId tenantId, String publicadaPor) {
}
