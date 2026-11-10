package br.com.smartkds.pedido.shared.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "log_auditoria")
public class LogAuditoriaEntity {

    @Id
    UUID id;
    String tenantId;
    String usuarioId;
    String acao;
    UUID comandaId;
    /** JSON em texto: TEXT + CHECK IS JSON (PostgreSQL) ou NVARCHAR(MAX) + CHECK ISJSON (Azure SQL). */
    String detalhe;
    String correlationId;
    Instant ocorridoEm;

    protected LogAuditoriaEntity() {
    }
}
