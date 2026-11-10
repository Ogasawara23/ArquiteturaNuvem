package br.com.smartkds.producao.features.avancarstatusticket;

import br.com.smartkds.producao.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Tickets")
class AvancarStatusTicketEndpoint {

    private final AvancarStatusTicketHandler handler;

    AvancarStatusTicketEndpoint(AvancarStatusTicketHandler handler) {
        this.handler = handler;
    }

    @PatchMapping("/interno/tickets/{ticketId}/status")
    @Operation(summary = "RECEBIDO -> EM_PREPARO -> PRONTO, validado pelo agregado (RF13)")
    public AvancarStatusTicketResponse avancar(@PathVariable UUID ticketId,
                                               @Valid @RequestBody AvancarStatusTicketRequest r) {
        return handler.executar(new AvancarStatusTicketCommand(ContextoDoTenant.atual(), ticketId, r.status()));
    }
}
