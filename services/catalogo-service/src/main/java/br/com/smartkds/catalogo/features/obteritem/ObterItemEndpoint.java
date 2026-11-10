package br.com.smartkds.catalogo.features.obteritem;

import br.com.smartkds.catalogo.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Itens do cardapio")
class ObterItemEndpoint {

    private final ObterItemQuery query;

    ObterItemEndpoint(ObterItemQuery query) {
        this.query = query;
    }

    @GetMapping("/interno/itens/{itemId}")
    @Operation(summary = "Detalha um item, com grupos de modificadores")
    public ObterItemResponse obter(@PathVariable String itemId) {
        return query.executar(ContextoDoTenant.atual(), itemId);
    }
}
