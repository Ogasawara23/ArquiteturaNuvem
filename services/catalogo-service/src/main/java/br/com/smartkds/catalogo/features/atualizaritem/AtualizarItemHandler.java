package br.com.smartkds.catalogo.features.atualizaritem;

import br.com.smartkds.catalogo.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.catalogo.shared.persistencia.ItemCardapioRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
class AtualizarItemHandler {

    private final ItemCardapioRepository repositorio;
    private final Clock relogio;

    AtualizarItemHandler(ItemCardapioRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    public AtualizarItemResponse executar(AtualizarItemCommand comando) {
        var item = repositorio.buscarPorId(comando.tenantId(), comando.itemId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item", comando.itemId()));
        item.atualizar(comando.dados(), Instant.now(relogio));
        repositorio.salvar(item);
        return new AtualizarItemResponse(item.id(), item.atualizadoEm());
    }
}
