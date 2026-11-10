package br.com.smartkds.estabelecimento.features.listarestacoes;

import br.com.smartkds.estabelecimento.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Estacoes")
class ListarEstacoesEndpoint {

    private final ListarEstacoesQuery query;

    ListarEstacoesEndpoint(ListarEstacoesQuery query) {
        this.query = query;
    }

    @GetMapping("/interno/estacoes")
    @Operation(summary = "Estacoes do estabelecimento em ordem de exibicao")
    public List<EstacaoDto> listar() {
        return query.listar(ContextoDoTenant.atual());
    }

    @GetMapping("/interno/estacoes/{codigo}")
    @Operation(summary = "Estacao pelo codigo, com o percentual de atencao do SLA (usado pelo bff-kds)")
    public EstacaoDto obter(@PathVariable String codigo) {
        return query.obter(ContextoDoTenant.atual(), codigo);
    }
}
