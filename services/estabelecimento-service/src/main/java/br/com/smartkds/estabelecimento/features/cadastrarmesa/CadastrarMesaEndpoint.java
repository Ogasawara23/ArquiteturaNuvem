package br.com.smartkds.estabelecimento.features.cadastrarmesa;

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
@Tag(name = "Mesas")
class CadastrarMesaEndpoint {

    private final CadastrarMesaHandler handler;

    CadastrarMesaEndpoint(CadastrarMesaHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/interno/mesas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra a mesa e gera o qrToken opaco (RF19)")
    public CadastrarMesaResponse cadastrar(@Valid @RequestBody CadastrarMesaRequest r) {
        return handler.executar(new CadastrarMesaCommand(ContextoDoTenant.atual(), r.rotulo()));
    }
}
