package br.com.smartkds.producao.shared.dominio;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Servico de dominio (Secao 8.5). Ordem de precedencia:
 * 1. regra especifica para o itemCardapioId; 2. regra por categoria do cardapio;
 * 3. estacaoSugerida congelada no evento; 4. estacao padrao do estabelecimento.
 */
public final class RoteadorDeEstacao {

    private static final String ESTACAO_DE_CONTINGENCIA = "geral";

    private RoteadorDeEstacao() { }

    public static Map<String, List<ItemParaProduzir>> agruparPorEstacao(RegraRoteamento regra,
                                                                       List<ItemParaProduzir> itens) {
        return itens.stream().collect(Collectors.groupingBy(
                item -> estacaoDe(regra, item), LinkedHashMap::new, Collectors.toList()));
    }

    public static String estacaoDe(RegraRoteamento regra, ItemParaProduzir item) {
        return melhorRegra(regra, r -> r.itemCardapioId() != null && r.itemCardapioId().equals(item.itemCardapioId()))
                .or(() -> melhorRegra(regra, r -> r.itemCardapioId() == null && r.categoria() != null
                        && r.categoria().equalsIgnoreCase(item.categoria())))
                .or(() -> Optional.ofNullable(item.estacaoSugerida())
                        .filter(s -> !s.isBlank()).map(s -> s.trim().toLowerCase()))
                .or(() -> Optional.ofNullable(regra.estacaoPadrao()))
                .orElse(ESTACAO_DE_CONTINGENCIA);
    }

    private static Optional<String> melhorRegra(RegraRoteamento regra, Predicate<RegraRoteamento.Regra> criterio) {
        return regra.regras().stream()
                .filter(criterio)
                .max(Comparator.comparingInt(RegraRoteamento.Regra::prioridade))
                .map(RegraRoteamento.Regra::estacaoId);
    }
}
