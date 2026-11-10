package br.com.smartkds.pedido.shared.inbox;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evento_processado")
public class EventoProcessadoEntity {

    @Id
    UUID eventoId;
    String origem;
    Instant processadoEm;

    protected EventoProcessadoEntity() {
    }

    EventoProcessadoEntity(UUID eventoId, String origem, Instant processadoEm) {
        this.eventoId = eventoId;
        this.origem = origem;
        this.processadoEm = processadoEm;
    }
}
