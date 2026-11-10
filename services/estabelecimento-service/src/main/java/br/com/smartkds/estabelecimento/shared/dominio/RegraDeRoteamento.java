package br.com.smartkds.estabelecimento.shared.dominio;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Item ou categoria do cardapio -> estacao (RF19). Exatamente um criterio por regra. */
public record RegraDeRoteamento(UUID id,
                                TenantId tenantId,
                                String itemCardapioId,
                                String categoria,
                                String estacaoCodigo,
                                int prioridade) {

    public RegraDeRoteamento {
        boolean porItem = itemCardapioId != null && !itemCardapioId.isBlank();
        boolean porCategoria = categoria != null && !categoria.isBlank();
        if (porItem == porCategoria) {
            throw new RegraDeNegocioException("a regra deve informar itemCardapioId OU categoria");
        }
        if (estacaoCodigo == null || estacaoCodigo.isBlank()) {
            throw new RegraDeNegocioException("estacao de destino e obrigatoria");
        }
        itemCardapioId = porItem ? itemCardapioId : null;
        categoria = porCategoria ? categoria.trim() : null;
        estacaoCodigo = Estacao.normalizarCodigo(estacaoCodigo);
    }

    /** Garante que todas as regras apontam para estacoes existentes do tenant. */
    public static void validarDestinos(Collection<RegraDeRoteamento> regras, Collection<Estacao> estacoes) {
        Set<String> codigos = estacoes.stream().map(Estacao::codigo).collect(Collectors.toSet());
        regras.stream().filter(r -> !codigos.contains(r.estacaoCodigo())).findFirst().ifPresent(r -> {
            throw new RegraDeNegocioException("estacao inexistente no estabelecimento: " + r.estacaoCodigo());
        });
    }
}
