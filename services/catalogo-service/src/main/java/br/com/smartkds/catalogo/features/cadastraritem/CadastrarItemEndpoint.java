package br.com.smartkds.catalogo.features.cadastraritem;

import br.com.smartkds.catalogo.shared.dominio.DadosDoItem;
import br.com.smartkds.catalogo.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@Tag(name = "Itens do cardapio")
class CadastrarItemEndpoint {

    private final CadastrarItemHandler handler;

    CadastrarItemEndpoint(CadastrarItemHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/interno/itens")
    @Operation(summary = "Cadastra um item no catalogo (RF02)")
    public ResponseEntity<CadastrarItemResponse> cadastrar(@Valid @RequestBody CadastrarItemRequest r) {
        var comando = new CadastrarItemCommand(ContextoDoTenant.atual(), new DadosDoItem(
                r.nome(), r.descricao(), r.categoria(), r.precoCentavos(), r.estacaoSugerida(),
                r.tempoAlvoPreparoSegundos(), r.imagemUrl(), r.gruposModificadores()));
        CadastrarItemResponse resposta = handler.executar(comando);
        return ResponseEntity.created(URI.create("/interno/itens/" + resposta.id())).body(resposta);
    }
}
