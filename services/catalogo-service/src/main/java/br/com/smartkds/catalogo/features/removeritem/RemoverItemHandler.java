package br.com.smartkds.catalogo.features.removeritem;

import br.com.smartkds.catalogo.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.catalogo.shared.persistencia.ItemCardapioRepository;
import org.springframework.stereotype.Component;

@Component
class RemoverItemHandler {

    private final ItemCardapioRepository repositorio;

    RemoverItemHandler(ItemCardapioRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void executar(RemoverItemCommand comando) {
        if (!repositorio.remover(comando.tenantId(), comando.itemId())) {
            throw new RecursoNaoEncontradoException("Item", comando.itemId());
        }
    }
}
