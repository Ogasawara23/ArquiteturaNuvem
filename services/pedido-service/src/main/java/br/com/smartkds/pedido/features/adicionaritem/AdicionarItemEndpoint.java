package br.com.smartkds.pedido.features.adicionaritem;

import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.SelecaoDeModificador;
import br.com.smartkds.pedido.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Comandas")
class AdicionarItemEndpoint {

    private final AdicionarItemHandler handler;

    AdicionarItemEndpoint(AdicionarItemHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/interno/comandas/{comandaId}/itens")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona item PENDENTE congelando preco e descricao (RF06)")
    public AdicionarItemResponse adicionar(@PathVariable UUID comandaId, @Valid @RequestBody AdicionarItemRequest r) {
        List<SelecaoDeModificador> selecoes = r.modificadores() == null ? List.of()
                : r.modificadores().stream().map(m -> new SelecaoDeModificador(m.grupo(), m.opcao())).toList();
        return handler.executar(new AdicionarItemCommand(ContextoDoTenant.atual(), new ComandaId(comandaId),
                r.itemCardapioId(), r.quantidade(), selecoes, r.observacao()));
    }
}
