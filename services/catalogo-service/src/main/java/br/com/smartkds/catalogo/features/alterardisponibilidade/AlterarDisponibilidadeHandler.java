package br.com.smartkds.catalogo.features.alterardisponibilidade;

import br.com.smartkds.catalogo.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.catalogo.shared.persistencia.ItemCardapioRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
class AlterarDisponibilidadeHandler {

    private final ItemCardapioRepository repositorio;
    private final Clock relogio;

    AlterarDisponibilidadeHandler(ItemCardapioRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    public AlterarDisponibilidadeResponse executar(AlterarDisponibilidadeCommand comando) {
        var item = repositorio.buscarPorId(comando.tenantId(), comando.itemId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item", comando.itemId()));
        item.alterarDisponibilidade(comando.disponivel(), Instant.now(relogio));
        repositorio.salvar(item);
        return new AlterarDisponibilidadeResponse(item.id(), item.disponivel(), item.atualizadoEm());
    }
}
