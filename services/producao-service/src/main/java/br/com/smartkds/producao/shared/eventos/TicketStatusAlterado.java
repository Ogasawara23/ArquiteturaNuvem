package br.com.smartkds.producao.shared.eventos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Evento ticket.status: atualiza o KDS e o progresso do cliente (EM_PREPARO, PRONTO, CANCELADO). */
public record TicketStatusAlterado(UUID eventoId,
                                   String tipo,
                                   int versaoDoContrato,
                                   String tenantId,
                                   UUID ticketId,
                                   UUID comandaId,
                                   String estacaoId,
                                   String status,
                                   List<UUID> itemComandaIds,
                                   Instant ocorridoEm) implements EventoDeDominio {

    public static final String ROUTING_KEY = "ticket.status";

    public static TicketStatusAlterado novo(String tenantId, UUID ticketId, UUID comandaId, String estacaoId,
                                            String status, List<UUID> itemComandaIds, Instant agora) {
        return new TicketStatusAlterado(UUID.randomUUID(), "TicketStatusAlterado", 1, tenantId, ticketId, comandaId,
                estacaoId, status, List.copyOf(itemComandaIds), agora);
    }

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }
}
