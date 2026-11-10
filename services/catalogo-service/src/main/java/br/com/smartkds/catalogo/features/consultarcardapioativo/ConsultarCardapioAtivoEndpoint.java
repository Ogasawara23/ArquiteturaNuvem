package br.com.smartkds.catalogo.features.consultarcardapioativo;

import br.com.smartkds.catalogo.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Versoes do cardapio")
class ConsultarCardapioAtivoEndpoint {

    private final CardapioAtivoQuery query;

    ConsultarCardapioAtivoEndpoint(CardapioAtivoQuery query) {
        this.query = query;
    }

    @GetMapping("/interno/cardapios/ativo")
    @Operation(summary = "Cardapio ativo do tenant agrupado por categoria (RF04)")
    public CardapioAtivoResponse consultar() {
        return query.executar(ContextoDoTenant.atual());
    }
}
