package br.com.smartkds.pedido.shared.eventos;

import java.time.Instant;
import java.util.UUID;

/** Evento comanda.fechada: encerra o acompanhamento do cliente e alimenta indicadores. */
public record ComandaFechada(UUID eventoId,
                             String tipo,
                             int versaoDoContrato,
                             String tenantId,
                             UUID comandaId,
                             String mesaId,
                             String mesaRotulo,
                             long subtotalCentavos,
                             long taxaServicoCentavos,
                             long totalCentavos,
                             int quantidadeDeItens,
                             Instant ocorridoEm) implements EventoDeDominio {

    public static final String ROUTING_KEY = "comanda.fechada";

    @SuppressWarnings("java:S107")
    public static ComandaFechada novo(String tenantId, UUID comandaId, String mesaId, String mesaRotulo,
                                      long subtotal, long taxa, long total, int quantidade, Instant ocorridoEm) {
        return new ComandaFechada(UUID.randomUUID(), "ComandaFechada", 1, tenantId, comandaId, mesaId,
                mesaRotulo, subtotal, taxa, total, quantidade, ocorridoEm);
    }

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }
}
