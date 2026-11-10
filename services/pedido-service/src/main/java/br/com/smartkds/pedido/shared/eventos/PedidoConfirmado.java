package br.com.smartkds.pedido.shared.eventos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Evento pedido.confirmado (Secao 8.4). */
public record PedidoConfirmado(UUID eventoId,
                               String tipo,
                               int versaoDoContrato,
                               String tenantId,
                               UUID comandaId,
                               String mesaId,
                               String mesaRotulo,
                               String chaveIdempotencia,
                               String versaoCardapioId,
                               Instant ocorridoEm,
                               List<ItemConfirmadoPayload> itens) implements EventoDeDominio {

    public static final String ROUTING_KEY = "pedido.confirmado";

    @SuppressWarnings("java:S107")
    public static PedidoConfirmado novo(String tenantId, UUID comandaId, String mesaId, String mesaRotulo,
                                        String chaveIdempotencia, String versaoCardapioId, Instant ocorridoEm,
                                        List<ItemConfirmadoPayload> itens) {
        return new PedidoConfirmado(UUID.randomUUID(), "PedidoConfirmado", 1, tenantId, comandaId, mesaId,
                mesaRotulo, chaveIdempotencia, versaoCardapioId, ocorridoEm, List.copyOf(itens));
    }

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }
}
