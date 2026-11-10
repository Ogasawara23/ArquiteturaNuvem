package br.com.smartkds.catalogo.features.removeritem;

import br.com.smartkds.catalogo.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Itens do cardapio")
class RemoverItemEndpoint {

    private final RemoverItemHandler handler;

    RemoverItemEndpoint(RemoverItemHandler handler) {
        this.handler = handler;
    }

    @DeleteMapping("/interno/itens/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove o item do catalogo em edicao. Versoes publicadas mantem a copia imutavel.")
    public void remover(@PathVariable String itemId) {
        handler.executar(new RemoverItemCommand(ContextoDoTenant.atual(), itemId));
    }
}
