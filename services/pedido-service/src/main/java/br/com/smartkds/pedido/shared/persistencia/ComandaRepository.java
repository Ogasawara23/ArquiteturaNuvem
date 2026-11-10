package br.com.smartkds.pedido.shared.persistencia;

import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.MesaId;
import br.com.smartkds.pedido.shared.dominio.StatusComanda;
import br.com.smartkds.pedido.shared.dominio.TenantId;

import java.util.List;
import java.util.Optional;

/** Porta de persistencia do agregado. TenantId sempre e o primeiro parametro (verificado por ArchUnit). */
public interface ComandaRepository {

    Comanda salvar(Comanda comanda);

    Optional<Comanda> buscarPorId(TenantId tenantId, ComandaId comandaId);

    Optional<Comanda> buscarAbertaPorMesa(TenantId tenantId, MesaId mesaId);

    List<Comanda> listar(TenantId tenantId, StatusComanda statusOuNulo, MesaId mesaOuNulo);
}
