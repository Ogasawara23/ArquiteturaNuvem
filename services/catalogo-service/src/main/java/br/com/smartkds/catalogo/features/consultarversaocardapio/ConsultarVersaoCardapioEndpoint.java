package br.com.smartkds.catalogo.features.consultarversaocardapio;

import br.com.smartkds.catalogo.shared.dominio.ItemNaVersao;
import br.com.smartkds.catalogo.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Versoes do cardapio")
class ConsultarVersaoCardapioEndpoint {

    private final VersaoCardapioQuery query;

    ConsultarVersaoCardapioEndpoint(VersaoCardapioQuery query) {
        this.query = query;
    }

    @GetMapping("/interno/cardapios/{versaoId}")
    @Operation(summary = "Documento fechado de uma versao publicada")
    public VersaoCardapioResponse versao(@PathVariable String versaoId) {
        return query.versao(ContextoDoTenant.atual(), versaoId);
    }

    @GetMapping("/interno/cardapios/{versaoId}/itens/{itemId}")
    @Operation(summary = "Snapshot de um item em uma versao (usado pelo pedido-service para congelar o preco)")
    public ItemNaVersao item(@PathVariable String versaoId, @PathVariable String itemId) {
        return query.item(ContextoDoTenant.atual(), versaoId, itemId);
    }
}
