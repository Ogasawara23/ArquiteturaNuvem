package br.com.smartkds.catalogo.features;

import br.com.smartkds.catalogo.shared.dominio.ItemCardapio;
import br.com.smartkds.catalogo.shared.dominio.TenantId;
import br.com.smartkds.catalogo.shared.dominio.VersaoCardapio;
import br.com.smartkds.catalogo.shared.persistencia.ItemCardapioRepository;
import br.com.smartkds.catalogo.shared.persistencia.VersaoCardapioRepository;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fakes das portas de persistencia: os testes de fatia rodam sem Spring e sem MongoDB. */
public final class RepositoriosEmMemoria {

    private RepositoriosEmMemoria() { }

    public static final class Itens implements ItemCardapioRepository {
        public final Map<String, ItemCardapio> dados = new LinkedHashMap<>();

        @Override
        public ItemCardapio salvar(ItemCardapio item) {
            dados.put(item.id(), item);
            return item;
        }

        @Override
        public Optional<ItemCardapio> buscarPorId(TenantId tenantId, String itemId) {
            return Optional.ofNullable(dados.get(itemId)).filter(i -> i.tenantId().equals(tenantId));
        }

        @Override
        public List<ItemCardapio> listar(TenantId tenantId, String categoria) {
            return dados.values().stream()
                    .filter(i -> i.tenantId().equals(tenantId))
                    .filter(i -> categoria == null || i.categoria().equals(categoria))
                    .toList();
        }

        @Override
        public boolean remover(TenantId tenantId, String itemId) {
            return buscarPorId(tenantId, itemId).map(i -> dados.remove(itemId) != null).orElse(false);
        }
    }

    public static final class Versoes implements VersaoCardapioRepository {
        public final Map<String, VersaoCardapio> dados = new HashMap<>();
        public final Map<String, String> ativas = new HashMap<>();

        @Override
        public VersaoCardapio salvar(VersaoCardapio versao) {
            dados.put(versao.versaoId(), versao);
            return versao;
        }

        @Override
        public Optional<VersaoCardapio> buscarPorId(TenantId tenantId, String versaoId) {
            return Optional.ofNullable(dados.get(versaoId)).filter(v -> v.tenantId().equals(tenantId));
        }

        @Override
        public Optional<VersaoCardapio> buscarAtiva(TenantId tenantId) {
            return Optional.ofNullable(ativas.get(tenantId.valor())).flatMap(id -> buscarPorId(tenantId, id));
        }

        @Override
        public void definirAtiva(TenantId tenantId, String versaoId) {
            ativas.put(tenantId.valor(), versaoId);
        }
    }
}
