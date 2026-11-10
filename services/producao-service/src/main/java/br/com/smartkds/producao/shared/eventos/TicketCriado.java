package br.com.smartkds.producao.shared.eventos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Evento ticket.criado: insere o cartao na tela da estacao (Figura 17). */
public record TicketCriado(UUID eventoId,
                           String tipo,
                           int versaoDoContrato,
                           String tenantId,
                           UUID ticketId,
                           UUID comandaId,
                           String mesaRotulo,
                           String estacaoId,
                           String status,
                           int tempoAlvoSegundos,
                           Instant criadoEm,
                           List<Item> itens,
                           Instant ocorridoEm) implements EventoDeDominio {

    public static final String ROUTING_KEY = "ticket.criado";

    public record Item(UUID itemComandaId, String descricao, int quantidade, List<String> modificadores,
                       String observacao) {
    }

    @SuppressWarnings("java:S107")
    public static TicketCriado novo(String tenantId, UUID ticketId, UUID comandaId, String mesaRotulo,
                                    String estacaoId, String status, int tempoAlvo, Instant criadoEm,
                                    List<Item> itens) {
        return new TicketCriado(UUID.randomUUID(), "TicketCriado", 1, tenantId, ticketId, comandaId, mesaRotulo,
                estacaoId, status, tempoAlvo, criadoEm, List.copyOf(itens), criadoEm);
    }

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }
}
