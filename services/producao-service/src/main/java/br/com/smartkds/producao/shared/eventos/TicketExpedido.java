package br.com.smartkds.producao.shared.eventos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Evento ticket.expedido: o pedido-service marca os itens como ENTREGUE (Figura 18). */
public record TicketExpedido(UUID eventoId,
                             String tipo,
                             int versaoDoContrato,
                             String tenantId,
                             UUID ticketId,
                             UUID comandaId,
                             String estacaoId,
                             List<UUID> itemComandaIds,
                             Instant ocorridoEm) implements EventoDeDominio {

    public static final String ROUTING_KEY = "ticket.expedido";

    public static TicketExpedido novo(String tenantId, UUID ticketId, UUID comandaId, String estacaoId,
                                      List<UUID> itemComandaIds, Instant agora) {
        return new TicketExpedido(UUID.randomUUID(), "TicketExpedido", 1, tenantId, ticketId, comandaId, estacaoId,
                List.copyOf(itemComandaIds), agora);
    }

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }
}
