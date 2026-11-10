package br.com.smartkds.pedido.features.consultarauditoria;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

record RegistroAuditoriaDto(UUID id, String usuarioId, String acao, UUID comandaId,
                            Map<String, Object> detalhe, String correlationId, Instant ocorridoEm) {
}
