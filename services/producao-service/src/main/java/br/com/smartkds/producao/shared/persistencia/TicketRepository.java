package br.com.smartkds.producao.shared.persistencia;

import br.com.smartkds.producao.shared.dominio.TenantId;
import br.com.smartkds.producao.shared.dominio.TicketProducao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository {

    void salvarTodos(List<TicketProducao> tickets);

    TicketProducao salvar(TicketProducao ticket);

    Optional<TicketProducao> buscarPorId(TenantId tenantId, UUID ticketId);

    List<TicketProducao> listarAtivosPorEstacao(TenantId tenantId, String estacaoId);

    List<TicketProducao> buscarPorItemDaComanda(TenantId tenantId, UUID itemComandaId);
}
