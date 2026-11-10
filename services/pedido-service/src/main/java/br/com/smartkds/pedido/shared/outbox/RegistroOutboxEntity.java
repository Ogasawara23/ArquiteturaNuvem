package br.com.smartkds.pedido.shared.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_evento")
public class RegistroOutboxEntity {

    @Id
    UUID eventoId;
    String tenantId;
    String tipo;
    String routingKey;
    /** JSON em texto: TEXT + CHECK IS JSON (PostgreSQL) ou NVARCHAR(MAX) + CHECK ISJSON (Azure SQL). */
    String payload;
    String traceparent;
    Instant criadoEm;
    Instant publicadoEm;
    int tentativas;

    protected RegistroOutboxEntity() {
    }

    @SuppressWarnings("java:S107")
    RegistroOutboxEntity(UUID eventoId, String tenantId, String tipo, String routingKey, String payload,
                         String traceparent, Instant criadoEm) {
        this.eventoId = eventoId;
        this.tenantId = tenantId;
        this.tipo = tipo;
        this.routingKey = routingKey;
        this.payload = payload;
        this.traceparent = traceparent;
        this.criadoEm = criadoEm;
    }
}
