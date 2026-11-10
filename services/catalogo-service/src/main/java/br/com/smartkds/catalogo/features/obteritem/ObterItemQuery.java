package br.com.smartkds.catalogo.features.obteritem;

import br.com.smartkds.catalogo.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.catalogo.shared.dominio.TenantId;
import br.com.smartkds.catalogo.shared.persistencia.ItemCardapioRepository;
import org.springframework.stereotype.Component;

@Component
class ObterItemQuery {

    private final ItemCardapioRepository repositorio;

    ObterItemQuery(ItemCardapioRepository repositorio) {
        this.repositorio = repositorio;
    }

    public ObterItemResponse executar(TenantId tenantId, String itemId) {
        var i = repositorio.buscarPorId(tenantId, itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item", itemId));
        return new ObterItemResponse(i.id(), i.nome(), i.descricao(), i.categoria(), i.precoCentavos(),
                i.estacaoSugerida(), i.tempoAlvoPreparoSegundos(), i.disponivel(), i.imagemUrl(),
                i.gruposModificadores(), i.atualizadoEm());
    }
}
