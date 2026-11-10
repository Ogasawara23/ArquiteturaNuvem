package br.com.smartkds.catalogo.features.atualizaritem;

import br.com.smartkds.catalogo.shared.dominio.DadosDoItem;
import br.com.smartkds.catalogo.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Itens do cardapio")
class AtualizarItemEndpoint {

    private final AtualizarItemHandler handler;

    AtualizarItemEndpoint(AtualizarItemHandler handler) {
        this.handler = handler;
    }

    @PutMapping("/interno/itens/{itemId}")
    @Operation(summary = "Atualiza os dados de um item (RF02). Nao afeta versoes ja publicadas.")
    public AtualizarItemResponse atualizar(@PathVariable String itemId, @Valid @RequestBody AtualizarItemRequest r) {
        return handler.executar(new AtualizarItemCommand(ContextoDoTenant.atual(), itemId, new DadosDoItem(
                r.nome(), r.descricao(), r.categoria(), r.precoCentavos(), r.estacaoSugerida(),
                r.tempoAlvoPreparoSegundos(), r.imagemUrl(), r.gruposModificadores())));
    }
}
