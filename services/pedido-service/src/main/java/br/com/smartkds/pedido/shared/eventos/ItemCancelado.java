package br.com.smartkds.pedido.shared.eventos;

import java.time.Instant;
import java.util.UUID;

/** Evento pedido.item.cancelado: dispara a compensacao no contexto de producao (Figura 19). */
public record ItemCancelado(UUID eventoId,
                            String tipo,
                            int versaoDoContrato,
                            String tenantId,
                            UUID comandaId,
                            UUID itemComandaId,
                            String motivo,
                            Instant ocorridoEm) implements EventoDeDominio {

    public static final String ROUTING_KEY = "pedido.item.cancelado";

    public static ItemCancelado novo(String tenantId, UUID comandaId, UUID itemComandaId, String motivo,
                                     Instant ocorridoEm) {
        return new ItemCancelado(UUID.randomUUID(), "ItemCancelado", 1, tenantId, comandaId, itemComandaId,
                motivo, ocorridoEm);
    }

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }
}
