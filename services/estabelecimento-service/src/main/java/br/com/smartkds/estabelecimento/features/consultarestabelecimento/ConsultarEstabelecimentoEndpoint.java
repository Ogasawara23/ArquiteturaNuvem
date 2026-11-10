package br.com.smartkds.estabelecimento.features.consultarestabelecimento;

import br.com.smartkds.estabelecimento.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Estabelecimento")
class ConsultarEstabelecimentoEndpoint {

    private final EstabelecimentoQuery query;

    ConsultarEstabelecimentoEndpoint(EstabelecimentoQuery query) {
        this.query = query;
    }

    @GetMapping("/interno/estabelecimento")
    @Operation(summary = "Configuracao atual do estabelecimento, com contagem de mesas e estacoes")
    public EstabelecimentoDto consultar() {
        return query.executar(ContextoDoTenant.atual());
    }
}
