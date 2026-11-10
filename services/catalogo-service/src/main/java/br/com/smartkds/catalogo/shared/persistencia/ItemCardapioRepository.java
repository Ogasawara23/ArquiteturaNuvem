package br.com.smartkds.catalogo.shared.persistencia;

import br.com.smartkds.catalogo.shared.dominio.ItemCardapio;
import br.com.smartkds.catalogo.shared.dominio.TenantId;

import java.util.List;
import java.util.Optional;

/** Porta de persistencia do item. Todo metodo de leitura recebe o TenantId primeiro (Secao 8.2). */
public interface ItemCardapioRepository {

    ItemCardapio salvar(ItemCardapio item);

    Optional<ItemCardapio> buscarPorId(TenantId tenantId, String itemId);

    List<ItemCardapio> listar(TenantId tenantId, String categoriaOuNulo);

    boolean remover(TenantId tenantId, String itemId);
}
