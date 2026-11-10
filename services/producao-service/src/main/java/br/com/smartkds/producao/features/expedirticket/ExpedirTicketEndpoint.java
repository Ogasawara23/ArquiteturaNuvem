package br.com.smartkds.producao.features.expedirticket;

import br.com.smartkds.producao.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Tickets")
class ExpedirTicketEndpoint {

    private final ExpedirTicketHandler handler;

    ExpedirTicketEndpoint(ExpedirTicketHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/interno/tickets/{ticketId}/bump")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Bump: expede o ticket PRONTO e publica TicketExpedido via outbox (RF14)")
    public void expedir(@PathVariable UUID ticketId) {
        handler.executar(new ExpedirTicketCommand(ContextoDoTenant.atual(), ticketId));
    }
}
