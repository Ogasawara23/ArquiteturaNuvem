package br.com.smartkds.pedido.features.consultarcomanda;

import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.MesaId;
import br.com.smartkds.pedido.shared.dominio.StatusComanda;
import br.com.smartkds.pedido.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Comandas")
class ConsultarComandaEndpoint {

    private final ComandaProjectionQuery query;

    ConsultarComandaEndpoint(ComandaProjectionQuery query) {
        this.query = query;
    }

    @GetMapping("/interno/comandas/{comandaId}")
    @Operation(summary = "Estado atual da comanda e de cada item (RF09)")
    public ComandaDto obter(@PathVariable UUID comandaId) {
        return query.porId(ContextoDoTenant.atual(), new ComandaId(comandaId));
    }

    @GetMapping("/interno/comandas")
    @Operation(summary = "Lista comandas do tenant, filtrando por status e/ou mesa")
    public List<ComandaDto> listar(@RequestParam(required = false) StatusComanda status,
                                   @RequestParam(required = false) String mesaId) {
        return query.listar(ContextoDoTenant.atual(), status,
                mesaId == null || mesaId.isBlank() ? null : new MesaId(mesaId));
    }
}
