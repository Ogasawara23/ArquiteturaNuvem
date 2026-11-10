package br.com.smartkds.pedido.shared.eventos;

import java.time.Instant;
import java.util.UUID;

/** Contrato comum dos eventos publicados pelo pedido-service (AsyncAPI: contratos/asyncapi). */
public interface EventoDeDominio {

    UUID eventoId();

    String tipo();

    String tenantId();

    Instant ocorridoEm();

    /** Chave de roteamento no exchange smartkds.pedidos. */
    String routingKey();
}
