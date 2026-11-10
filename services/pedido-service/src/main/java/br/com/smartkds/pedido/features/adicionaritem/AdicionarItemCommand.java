package br.com.smartkds.pedido.features.adicionaritem;

import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.SelecaoDeModificador;
import br.com.smartkds.pedido.shared.dominio.TenantId;

import java.util.List;

record AdicionarItemCommand(TenantId tenantId,
                            ComandaId comandaId,
                            String itemCardapioId,
                            int quantidade,
                            List<SelecaoDeModificador> modificadores,
                            String observacao) {
}
