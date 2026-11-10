package br.com.smartkds.pedido.features.adicionaritem;

import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.dominio.ItemComanda;
import br.com.smartkds.pedido.shared.dominio.ItemIndisponivelException;
import br.com.smartkds.pedido.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.pedido.shared.integracao.CatalogoGateway;
import br.com.smartkds.pedido.shared.persistencia.ComandaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Figura 14. O snapshot vem da versao ATIVA no instante da adicao; a partir dai
 * preco e descricao ficam congelados no item. Nenhum evento e publicado: item PENDENTE
 * ainda nao existe para a cozinha.
 */
@Component
class AdicionarItemHandler {

    private final ComandaRepository repositorio;
    private final CatalogoGateway catalogo;
    private final AdicionarItemValidator validador = new AdicionarItemValidator();
    private final Clock relogio;

    AdicionarItemHandler(ComandaRepository repositorio, CatalogoGateway catalogo, Clock relogio) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
        this.relogio = relogio;
    }

    @Transactional
    public AdicionarItemResponse executar(AdicionarItemCommand comando) {
        validador.validar(comando);
        Comanda comanda = repositorio.buscarPorId(comando.tenantId(), comando.comandaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comanda", comando.comandaId().valor()));
        String versao = catalogo.versaoAtiva(comando.tenantId()).orElse(comanda.versaoCardapioId());
        var snapshot = catalogo.obterItemDaVersao(comando.tenantId(), versao, comando.itemCardapioId())
                .orElseThrow(() -> new ItemIndisponivelException(comando.itemCardapioId()));

        ItemComanda item = comanda.adicionarItem(snapshot, comando.quantidade(), comando.modificadores(),
                comando.observacao(), Instant.now(relogio));
        repositorio.salvar(comanda);

        return new AdicionarItemResponse(item.id().valor(), item.descricao(), item.precoUnitario().centavos(),
                item.total().centavos(), comanda.subtotal().centavos());
    }
}
