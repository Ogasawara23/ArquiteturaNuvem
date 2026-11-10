package br.com.smartkds.pedido.shared.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class EventoProcessadoJpa implements EventoProcessadoRepository {

    interface Dados extends JpaRepository<EventoProcessadoEntity, UUID> {
        @Modifying
        @Query("delete from EventoProcessadoEntity e where e.processadoEm < :limite")
        int expurgarAnterioresA(@Param("limite") Instant limite);
    }

    private final Dados dados;
    private final Clock relogio;

    EventoProcessadoJpa(Dados dados, Clock relogio) {
        this.dados = dados;
        this.relogio = relogio;
    }

    @Override
    public boolean jaProcessado(UUID eventoId) {
        return dados.existsById(eventoId);
    }

    /** MANDATORY: a marcacao so faz sentido junto com o efeito do evento. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void marcarProcessado(UUID eventoId, String origem, Instant agora) {
        dados.save(new EventoProcessadoEntity(eventoId, origem, agora));
    }

    /** Registros com mais de sete dias sao expurgados diariamente. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void expurgar() {
        dados.expurgarAnterioresA(Instant.now(relogio).minus(Duration.ofDays(7)));
    }
}
