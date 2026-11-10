package br.com.smartkds.catalogo.features.listaritens;

import br.com.smartkds.catalogo.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Itens do cardapio")
class ListarItensEndpoint {

    private final ListarItensQuery query;

    ListarItensEndpoint(ListarItensQuery query) {
        this.query = query;
    }

    @GetMapping("/interno/itens")
    @Operation(summary = "Lista os itens em edicao do tenant, opcionalmente por categoria")
    public List<ItemResumoDto> listar(@RequestParam(required = false) String categoria) {
        return query.executar(ContextoDoTenant.atual(), categoria);
    }
}
