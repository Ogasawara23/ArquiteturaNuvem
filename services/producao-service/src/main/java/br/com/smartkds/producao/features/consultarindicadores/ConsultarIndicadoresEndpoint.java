package br.com.smartkds.producao.features.consultarindicadores;

import br.com.smartkds.producao.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Indicadores")
class ConsultarIndicadoresEndpoint {

    private final IndicadoresQuery query;

    ConsultarIndicadoresEndpoint(IndicadoresQuery query) {
        this.query = query;
    }

    @GetMapping("/interno/indicadores")
    @Operation(summary = "Tempo medio de preparo, atrasos e volume por hora nas ultimas N horas (RF18)")
    public IndicadoresDto consultar(@RequestParam(defaultValue = "24") int horas,
                                    @RequestParam(defaultValue = "America/Sao_Paulo") String fuso) {
        return query.executar(ContextoDoTenant.atual(), Math.min(Math.max(horas, 1), 24 * 31), fuso);
    }
}
