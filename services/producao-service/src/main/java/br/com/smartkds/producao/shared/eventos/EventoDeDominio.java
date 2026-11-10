package br.com.smartkds.producao.shared.eventos;

import java.time.Instant;
import java.util.UUID;

/** Contrato comum dos eventos publicados no exchange smartkds.producao. */
public interface EventoDeDominio {

    UUID eventoId();

    String tipo();

    String tenantId();

    Instant ocorridoEm();

    String routingKey();
}
