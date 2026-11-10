package br.com.smartkds.catalogo.features.consultarversaocardapio;

import br.com.smartkds.catalogo.shared.dominio.ItemNaVersao;
import br.com.smartkds.catalogo.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.catalogo.shared.dominio.TenantId;
import br.com.smartkds.catalogo.shared.dominio.VersaoCardapio;
import br.com.smartkds.catalogo.shared.persistencia.VersaoCardapioRepository;
import org.springframework.stereotype.Component;

@Component
class VersaoCardapioQuery {

    private final VersaoCardapioRepository versoes;

    VersaoCardapioQuery(VersaoCardapioRepository versoes) {
        this.versoes = versoes;
    }

    public VersaoCardapioResponse versao(TenantId tenantId, String versaoId) {
        VersaoCardapio v = carregar(tenantId, versaoId);
        return new VersaoCardapioResponse(v.versaoId(), v.publicadaEm(), v.publicadaPor(),
                v.quantidadeDeItens(), v.itens());
    }

    public ItemNaVersao item(TenantId tenantId, String versaoId, String itemId) {
        return carregar(tenantId, versaoId).item(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item na versao " + versaoId, itemId));
    }

    private VersaoCardapio carregar(TenantId tenantId, String versaoId) {
        return versoes.buscarPorId(tenantId, versaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Versao do cardapio", versaoId));
    }
}
