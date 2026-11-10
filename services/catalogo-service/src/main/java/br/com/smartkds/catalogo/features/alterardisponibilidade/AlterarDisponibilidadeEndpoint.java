package br.com.smartkds.catalogo.features.alterardisponibilidade;

import br.com.smartkds.catalogo.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Itens do cardapio")
class AlterarDisponibilidadeEndpoint {

    private final AlterarDisponibilidadeHandler handler;

    AlterarDisponibilidadeEndpoint(AlterarDisponibilidadeHandler handler) {
        this.handler = handler;
    }

    @PatchMapping("/interno/itens/{itemId}/disponibilidade")
    @Operation(summary = "Marca o item como disponivel ou esgotado (RF02)")
    public AlterarDisponibilidadeResponse alterar(@PathVariable String itemId,
                                                  @Valid @RequestBody AlterarDisponibilidadeRequest r) {
        return handler.executar(new AlterarDisponibilidadeCommand(ContextoDoTenant.atual(), itemId, r.disponivel()));
    }
}
