package br.com.smartkds.estabelecimento.features.resolverqrtoken;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Mesas")
class ResolverQrTokenEndpoint {

    private final ResolverQrTokenQuery query;

    ResolverQrTokenEndpoint(ResolverQrTokenQuery query) {
        this.query = query;
    }

    @GetMapping("/interno/mesas/por-token/{qrToken}")
    @Operation(summary = "Resolve o qrToken em (tenantId, mesaId). Nao exige X-Tenant-Id (Figura 13).")
    public MesaResolvidaDto resolver(@PathVariable String qrToken) {
        return query.executar(qrToken);
    }
}
