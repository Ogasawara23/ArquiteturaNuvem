package br.com.smartkds.pedido.features.fecharcomanda;

import br.com.smartkds.pedido.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@Tag(name = "Comandas")
class FecharComandaEndpoint {

    private static final BigDecimal TAXA_PADRAO = BigDecimal.TEN;

    private final FecharComandaHandler handler;

    FecharComandaEndpoint(FecharComandaHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/interno/comandas/{comandaId}/fechamento")
    @Operation(summary = "Fecha a comanda com subtotal, taxa de servico e total (RF10)")
    public FecharComandaResponse fechar(@PathVariable UUID comandaId,
                                        @RequestBody(required = false) FecharComandaRequest r) {
        BigDecimal taxa = r == null || r.percentualTaxaServico() == null ? TAXA_PADRAO : r.percentualTaxaServico();
        return handler.executar(new FecharComandaCommand(ContextoDoTenant.atual(), comandaId, taxa,
                ContextoDoTenant.usuario(), ContextoDoTenant.correlacao()));
    }
}
