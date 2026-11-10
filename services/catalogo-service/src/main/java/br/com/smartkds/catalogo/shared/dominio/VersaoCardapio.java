package br.com.smartkds.catalogo.shared.dominio;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Versao publicada e imutavel do cardapio (ADR-11). Depois de criada, nenhum metodo
 * altera o seu estado: e isso que permite reproduzir o preco de uma comanda antiga.
 */
public final class VersaoCardapio {

    private final String versaoId;
    private final TenantId tenantId;
    private final Instant publicadaEm;
    private final String publicadaPor;
    private final List<ItemNaVersao> itens;

    private VersaoCardapio(String versaoId, TenantId tenantId, Instant publicadaEm,
                           String publicadaPor, List<ItemNaVersao> itens) {
        this.versaoId = versaoId;
        this.tenantId = tenantId;
        this.publicadaEm = publicadaEm;
        this.publicadaPor = publicadaPor;
        this.itens = List.copyOf(itens);
    }

    /** Congela os itens DISPONIVEIS do tenant em uma nova versao. */
    public static VersaoCardapio publicar(TenantId tenantId, List<ItemCardapio> itensDoTenant,
                                          String publicadaPor, Instant agora) {
        List<ItemNaVersao> disponiveis = itensDoTenant.stream()
                .filter(item -> item.tenantId().equals(tenantId))
                .filter(ItemCardapio::disponivel)
                .sorted(Comparator.comparing(ItemCardapio::categoria).thenComparing(ItemCardapio::nome))
                .map(ItemCardapio::congelar)
                .toList();
        if (disponiveis.isEmpty()) {
            throw new VersaoInvalidaException("nao ha itens disponiveis para publicar");
        }
        String versaoId = "v-" + agora.toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
        String autor = publicadaPor == null || publicadaPor.isBlank() ? "desconhecido" : publicadaPor;
        return new VersaoCardapio(versaoId, tenantId, agora, autor, disponiveis);
    }

    public static VersaoCardapio restaurar(String versaoId, TenantId tenantId, Instant publicadaEm,
                                           String publicadaPor, List<ItemNaVersao> itens) {
        return new VersaoCardapio(versaoId, tenantId, publicadaEm, publicadaPor, itens);
    }

    public Optional<ItemNaVersao> item(String itemCardapioId) {
        return itens.stream().filter(i -> i.itemCardapioId().equals(itemCardapioId)).findFirst();
    }

    public String versaoId() { return versaoId; }
    public TenantId tenantId() { return tenantId; }
    public Instant publicadaEm() { return publicadaEm; }
    public String publicadaPor() { return publicadaPor; }
    public List<ItemNaVersao> itens() { return itens; }
    public int quantidadeDeItens() { return itens.size(); }
}
