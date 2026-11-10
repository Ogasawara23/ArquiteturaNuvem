package br.com.smartkds.producao.shared.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class ProducaoSpringData {

    private ProducaoSpringData() { }

    interface Tickets extends JpaRepository<TicketEntity, UUID> {
        Optional<TicketEntity> findByTenantIdAndId(String tenantId, UUID id);

        List<TicketEntity> findByTenantIdAndEstacaoIdAndStatusInOrderByCriadoEmAsc(String tenantId, String estacaoId,
                                                                                    Collection<String> status);

        @Query("select distinct t from TicketEntity t join t.itens i "
                + "where t.tenantId = :tenant and i.itemComandaId = :item")
        List<TicketEntity> buscarPorItemDaComanda(@Param("tenant") String tenantId, @Param("item") UUID itemComandaId);
    }
}
