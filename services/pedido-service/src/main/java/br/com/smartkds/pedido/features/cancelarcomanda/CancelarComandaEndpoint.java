package br.com.smartkds.pedido.features.cancelarcomanda;

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
class CancelarComandaEndpoint {

    private final CancelarComandaHandler handler;

    CancelarComandaEndpoint(CancelarComandaHandler handler) {
        this.handler = handler;
    }

    @DeleteMapping("/interno/comandas/{comandaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancela a comanda (exclusao logica). Recusado se algum item ja entrou em producao.")
    public void cancelar(@PathVariable UUID comandaId, @RequestParam(required = false) String motivo) {
        handler.executar(new CancelarComandaCommand(ContextoDoTenant.atual(), comandaId, motivo,
                ContextoDoTenant.usuario(), ContextoDoTenant.correlacao()));
    }
}
