package br.com.smartkds.pedido.features.abrircomanda;

import br.com.smartkds.pedido.shared.dominio.MesaId;
import br.com.smartkds.pedido.shared.dominio.TenantId;

record AbrirComandaCommand(TenantId tenantId, MesaId mesaId, String mesaRotulo) {
}
