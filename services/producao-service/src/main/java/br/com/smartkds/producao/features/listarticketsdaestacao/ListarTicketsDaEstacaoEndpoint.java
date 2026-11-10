package br.com.smartkds.producao.features.listarticketsdaestacao;

import br.com.smartkds.producao.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Tickets")
class ListarTicketsDaEstacaoEndpoint {

    private final TicketsDaEstacaoQuery query;

    ListarTicketsDaEstacaoEndpoint(TicketsDaEstacaoQuery query) {
        this.query = query;
    }

    @GetMapping("/interno/estacoes/{estacaoId}/tickets")
    @Operation(summary = "Tickets ativos da estacao, ordenados por urgencia, com SLA calculado no servidor (RF12, RF15)")
    public List<TicketDaEstacaoDto> listar(@PathVariable String estacaoId,
                                           @RequestParam(defaultValue = "70") int percentualAtencao) {
        return query.executar(ContextoDoTenant.atual(), estacaoId.toLowerCase(), percentualAtencao);
    }
}
