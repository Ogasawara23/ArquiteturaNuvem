package br.com.smartkds.producao.features.avancarstatusticket;

import br.com.smartkds.producao.shared.dominio.StatusTicket;
import jakarta.validation.constraints.NotNull;

record AvancarStatusTicketRequest(@NotNull StatusTicket status) {
}
