package br.com.smartkds.producao.shared.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface OutboxSpringData extends JpaRepository<RegistroOutboxEntity, UUID> {

    /** SKIP LOCKED permite varias replicas do relay sem publicacao duplicada no mesmo ciclo. */
    @Query(value = "SELECT * FROM outbox_evento WHERE publicado_em IS NULL ORDER BY criado_em "
            + "LIMIT :lote FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<RegistroOutboxEntity> travarPendentes(@Param("lote") int lote);

    @Query(value = "SELECT count(*) FROM outbox_evento WHERE publicado_em IS NULL", nativeQuery = true)
    long contarPendentes();
}
