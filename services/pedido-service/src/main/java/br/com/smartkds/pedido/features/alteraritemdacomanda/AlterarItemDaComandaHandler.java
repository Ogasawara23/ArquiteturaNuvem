package br.com.smartkds.pedido.features.alteraritemdacomanda;

import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.ItemComanda;
import br.com.smartkds.pedido.shared.dominio.ItemComandaId;
import br.com.smartkds.pedido.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.pedido.shared.persistencia.ComandaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Update do CRUD. Item PENDENTE ainda nao existe para a cozinha: nenhum evento e publicado. */
@Component
class AlterarItemDaComandaHandler {

    private final ComandaRepository repositorio;

    AlterarItemDaComandaHandler(ComandaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public AlterarItemDaComandaResponse executar(AlterarItemDaComandaCommand c) {
        Comanda comanda = repositorio.buscarPorId(c.tenantId(), new ComandaId(c.comandaId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comanda", c.comandaId()));
        ItemComanda item = comanda.alterarItem(new ItemComandaId(c.itemId()), c.quantidade(), c.observacao());
        repositorio.salvar(comanda);
        return new AlterarItemDaComandaResponse(item.id().valor(), item.quantidade(), item.observacao(),
                item.total().centavos(), comanda.subtotal().centavos());
    }
}
