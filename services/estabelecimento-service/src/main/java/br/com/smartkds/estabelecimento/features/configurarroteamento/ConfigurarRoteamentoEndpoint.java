package br.com.smartkds.estabelecimento.features.configurarroteamento;

import br.com.smartkds.estabelecimento.shared.dominio.RegraDeRoteamento;
import br.com.smartkds.estabelecimento.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Roteamento")
class ConfigurarRoteamentoEndpoint {

    private final ConfigurarRoteamentoHandler handler;

    ConfigurarRoteamentoEndpoint(ConfigurarRoteamentoHandler handler) {
        this.handler = handler;
    }

    @PutMapping("/interno/roteamento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Substitui as regras item/categoria -> estacao e a estacao padrao (RF19)")
    public void configurar(@Valid @RequestBody ConfigurarRoteamentoRequest r) {
        var tenant = ContextoDoTenant.atual();
        List<RegraDeRoteamento> regras = r.regras() == null ? List.of() : r.regras().stream()
                .map(x -> new RegraDeRoteamento(UUID.randomUUID(), tenant, x.itemCardapioId(), x.categoria(),
                        x.estacaoId(), x.prioridade() == null ? 0 : x.prioridade()))
                .toList();
        handler.executar(new ConfigurarRoteamentoCommand(tenant, r.estacaoPadrao(), regras));
    }
}
