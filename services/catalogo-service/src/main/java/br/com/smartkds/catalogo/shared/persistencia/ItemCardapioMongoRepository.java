package br.com.smartkds.catalogo.shared.persistencia;

import br.com.smartkds.catalogo.shared.dominio.DadosDoItem;
import br.com.smartkds.catalogo.shared.dominio.ItemCardapio;
import br.com.smartkds.catalogo.shared.dominio.TenantId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Adaptador MongoDB da porta {@link ItemCardapioRepository}. */
@Component
public class ItemCardapioMongoRepository implements ItemCardapioRepository {

    private final CatalogoSpringData.Itens itens;

    ItemCardapioMongoRepository(CatalogoSpringData.Itens itens) {
        this.itens = itens;
    }

    @Override
    public ItemCardapio salvar(ItemCardapio item) {
        itens.save(paraDocumento(item));
        return item;
    }

    @Override
    public Optional<ItemCardapio> buscarPorId(TenantId tenantId, String itemId) {
        return itens.findByTenantIdAndId(tenantId.valor(), itemId).map(ItemCardapioMongoRepository::paraDominio);
    }

    @Override
    public List<ItemCardapio> listar(TenantId tenantId, String categoriaOuNulo) {
        List<ItemDocumento> documentos = categoriaOuNulo == null || categoriaOuNulo.isBlank()
                ? itens.findByTenantIdOrderByCategoriaAscNomeAsc(tenantId.valor())
                : itens.findByTenantIdAndCategoriaOrderByNomeAsc(tenantId.valor(), categoriaOuNulo);
        return documentos.stream().map(ItemCardapioMongoRepository::paraDominio).toList();
    }

    @Override
    public boolean remover(TenantId tenantId, String itemId) {
        return itens.deleteByTenantIdAndId(tenantId.valor(), itemId) > 0;
    }

    private static ItemDocumento paraDocumento(ItemCardapio item) {
        return new ItemDocumento(item.id(), item.tenantId().valor(), item.nome(), item.descricao(),
                item.categoria(), item.precoCentavos(), item.estacaoSugerida(),
                item.tempoAlvoPreparoSegundos(), item.disponivel(), item.imagemUrl(),
                item.gruposModificadores(), item.atualizadoEm());
    }

    private static ItemCardapio paraDominio(ItemDocumento d) {
        DadosDoItem dados = new DadosDoItem(d.nome(), d.descricao(), d.categoria(), d.precoCentavos(),
                d.estacaoSugerida(), d.tempoAlvoPreparoSegundos(), d.imagemUrl(), d.gruposModificadores());
        return ItemCardapio.restaurar(d.id(), new TenantId(d.tenantId()), dados, d.disponivel(), d.atualizadoEm());
    }
}
