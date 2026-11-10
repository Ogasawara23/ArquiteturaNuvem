package br.com.smartkds.pedido.shared.dominio;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Registro de operacao sensivel (RF20): cancelamento de item e fechamento de comanda. */
public record RegistroAuditoria(UUID id,
                                TenantId tenantId,
                                String usuarioId,
                                String acao,
                                UUID comandaId,
                                Map<String, Object> detalhe,
                                String correlationId,
                                Instant ocorridoEm) {

    public static RegistroAuditoria de(TenantId tenantId, String usuarioId, String acao, UUID comandaId,
                                       Map<String, Object> detalhe, String correlationId, Instant agora) {
        return new RegistroAuditoria(UUID.randomUUID(), tenantId,
                usuarioId == null || usuarioId.isBlank() ? "anonimo" : usuarioId,
                acao, comandaId, detalhe == null ? Map.of() : Map.copyOf(detalhe), correlationId, agora);
    }
}
