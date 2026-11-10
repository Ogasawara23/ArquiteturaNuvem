package br.com.smartkds.pedido.features.registrarentregadopedido;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Contrato consumido de smartkds.producao / ticket.expedido. Campos desconhecidos sao ignorados. */
record TicketExpedidoMessage(UUID eventoId,
                             String tenantId,
                             UUID ticketId,
                             UUID comandaId,
                             String estacaoId,
                             List<UUID> itemComandaIds,
                             Instant ocorridoEm) {
}
