package br.com.smartkds.pedido.features.alteraritemdacomanda;

import br.com.smartkds.pedido.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Comandas")
class AlterarItemDaComandaEndpoint {

    private final AlterarItemDaComandaHandler handler;

    AlterarItemDaComandaEndpoint(AlterarItemDaComandaHandler handler) {
        this.handler = handler;
    }

    @PatchMapping("/interno/comandas/{comandaId}/itens/{itemId}")
    @Operation(summary = "Altera quantidade e observacao de um item ainda no carrinho (PENDENTE)")
    public AlterarItemDaComandaResponse alterar(@PathVariable UUID comandaId, @PathVariable UUID itemId,
                                                @Valid @RequestBody AlterarItemDaComandaRequest r) {
        return handler.executar(new AlterarItemDaComandaCommand(ContextoDoTenant.atual(), comandaId, itemId,
                r.quantidade(), r.observacao()));
    }
}
