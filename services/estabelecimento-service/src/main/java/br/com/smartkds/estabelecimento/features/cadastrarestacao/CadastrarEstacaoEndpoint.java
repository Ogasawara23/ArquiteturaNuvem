package br.com.smartkds.estabelecimento.features.cadastrarestacao;

import br.com.smartkds.estabelecimento.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Estacoes")
class CadastrarEstacaoEndpoint {

    private final CadastrarEstacaoHandler handler;

    CadastrarEstacaoEndpoint(CadastrarEstacaoHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/interno/estacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra estacao de producao (grill, fritura, bar...) (RF19)")
    public CadastrarEstacaoResponse cadastrar(@Valid @RequestBody CadastrarEstacaoRequest r) {
        return handler.executar(new CadastrarEstacaoCommand(ContextoDoTenant.atual(), r.codigo(), r.nome(),
                r.ordemExibicao() == null ? 99 : r.ordemExibicao(),
                r.percentualAtencao() == null ? 70 : r.percentualAtencao()));
    }
}
