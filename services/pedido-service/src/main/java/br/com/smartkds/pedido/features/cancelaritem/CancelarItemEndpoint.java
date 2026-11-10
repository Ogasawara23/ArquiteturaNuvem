package br.com.smartkds.pedido.features.cancelaritem;

import br.com.smartkds.pedido.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Comandas")
class CancelarItemEndpoint {

    private final CancelarItemHandler handler;

    CancelarItemEndpoint(CancelarItemHandler handler) {
        this.handler = handler;
    }

    @DeleteMapping("/interno/comandas/{comandaId}/itens/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancela item PENDENTE ou CONFIRMADO; compensa o ticket via ItemCancelado (RF08)")
    public void cancelar(@PathVariable UUID comandaId, @PathVariable UUID itemId,
                         @RequestParam(required = false) String motivo) {
        handler.executar(new CancelarItemCommand(ContextoDoTenant.atual(), comandaId, itemId, motivo,
                ContextoDoTenant.usuario(), ContextoDoTenant.correlacao()));
    }
}
