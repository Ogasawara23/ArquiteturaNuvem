package br.com.smartkds.estabelecimento.features.consultarroteamento;

import br.com.smartkds.estabelecimento.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Roteamento")
class ConsultarRoteamentoEndpoint {

    private final RoteamentoQuery query;

    ConsultarRoteamentoEndpoint(RoteamentoQuery query) {
        this.query = query;
    }

    @GetMapping("/interno/roteamento")
    @Operation(summary = "Regra de roteamento vigente (consumida pelo producao-service com cache de 5 min)")
    public RoteamentoDto consultar() {
        return query.executar(ContextoDoTenant.atual());
    }
}
