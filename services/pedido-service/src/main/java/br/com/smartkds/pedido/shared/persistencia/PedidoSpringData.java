package br.com.smartkds.pedido.shared.persistencia;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Interfaces do Spring Data, invisiveis para as fatias. */
final class PedidoSpringData {

    private PedidoSpringData() { }

    interface Comandas extends JpaRepository<ComandaEntity, UUID> {
        Optional<ComandaEntity> findByTenantIdAndId(String tenantId, UUID id);

        Optional<ComandaEntity> findFirstByTenantIdAndMesaIdAndStatus(String tenantId, String mesaId, String status);

        List<ComandaEntity> findTop200ByTenantIdOrderByAbertaEmDesc(String tenantId);

        List<ComandaEntity> findTop200ByTenantIdAndStatusOrderByAbertaEmDesc(String tenantId, String status);
    }

    interface Auditoria extends JpaRepository<LogAuditoriaEntity, UUID> {
        List<LogAuditoriaEntity> findByTenantIdOrderByOcorridoEmDesc(String tenantId, Pageable pagina);
    }
}
