package br.com.smartkds.catalogo.features.publicarversaocardapio;

import br.com.smartkds.catalogo.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Versoes do cardapio")
class PublicarVersaoCardapioEndpoint {

    private final PublicarVersaoCardapioHandler handler;

    PublicarVersaoCardapioEndpoint(PublicarVersaoCardapioHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/interno/cardapios/versoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publica uma nova versao imutavel e a torna ativa (RF03)")
    public PublicarVersaoCardapioResponse publicar(@RequestBody(required = false) PublicarVersaoCardapioRequest r) {
        String autor = r == null ? null : r.publicadaPor();
        return handler.executar(new PublicarVersaoCardapioCommand(ContextoDoTenant.atual(), autor));
    }
}
