package br.com.smartkds.producao.features.avancarstatusticket;

import java.time.Instant;
import java.util.UUID;

record AvancarStatusTicketResponse(UUID ticketId, String status, Instant alteradoEm) {
}
