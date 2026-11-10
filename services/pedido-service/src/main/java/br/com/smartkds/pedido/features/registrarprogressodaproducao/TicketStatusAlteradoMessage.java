package br.com.smartkds.pedido.features.registrarprogressodaproducao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Contrato consumido de smartkds.producao / ticket.status. */
record TicketStatusAlteradoMessage(UUID eventoId,
                                   String tenantId,
                                   UUID ticketId,
                                   UUID comandaId,
                                   String estacaoId,
                                   String status,
                                   List<UUID> itemComandaIds,
                                   Instant ocorridoEm) {
}
