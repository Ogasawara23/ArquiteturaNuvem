package br.com.smartkds.estabelecimento.features.listarmesas;

import br.com.smartkds.estabelecimento.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Mesas")
class ListarMesasEndpoint {

    private final ListarMesasQuery query;

    ListarMesasEndpoint(ListarMesasQuery query) {
        this.query = query;
    }

    @GetMapping("/interno/mesas")
    @Operation(summary = "Mesas do estabelecimento com seus qrTokens")
    public List<MesaDto> listar() {
        return query.executar(ContextoDoTenant.atual());
    }
}
