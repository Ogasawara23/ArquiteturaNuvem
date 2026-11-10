package br.com.smartkds.pedido.shared.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.UUID;

interface OutboxSpringData extends JpaRepository<RegistroOutboxEntity, UUID> {

    /**
     * Trava os eventos pendentes pulando os ja travados por outra replica do relay.
     * lock.timeout = -2 (SKIP_LOCKED) gera "FOR UPDATE SKIP LOCKED" no PostgreSQL e
     * "WITH (UPDLOCK, ROWLOCK, READPAST)" no Azure SQL: a mesma consulta nos dois bancos.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select r from RegistroOutboxEntity r where r.publicadoEm is null order by r.criadoEm")
    List<RegistroOutboxEntity> travarPendentes(Pageable lote);

    @Query("select count(r) from RegistroOutboxEntity r where r.publicadoEm is null")
    long contarPendentes();
}
