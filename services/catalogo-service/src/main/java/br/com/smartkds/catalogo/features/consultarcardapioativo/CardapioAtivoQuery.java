package br.com.smartkds.catalogo.features.consultarcardapioativo;

import br.com.smartkds.catalogo.shared.dominio.ItemNaVersao;
import br.com.smartkds.catalogo.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.catalogo.shared.dominio.TenantId;
import br.com.smartkds.catalogo.shared.persistencia.VersaoCardapioRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
class CardapioAtivoQuery {

    private final VersaoCardapioRepository versoes;

    CardapioAtivoQuery(VersaoCardapioRepository versoes) {
        this.versoes = versoes;
    }

    public CardapioAtivoResponse executar(TenantId tenantId) {
        var versao = versoes.buscarAtiva(tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cardapio ativo do tenant", tenantId.valor()));
        List<CardapioAtivoResponse.CategoriaDto> categorias = versao.itens().stream()
                .collect(Collectors.groupingBy(ItemNaVersao::categoria, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new CardapioAtivoResponse.CategoriaDto(e.getKey(), e.getValue()))
                .toList();
        return new CardapioAtivoResponse(versao.versaoId(), versao.publicadaEm(), categorias);
    }
}
