package br.com.smartkds.producao.shared.dominio;

import java.util.List;

/** Configuracao de roteamento vigente do tenant (copia lida do estabelecimento-service). */
public record RegraRoteamento(String estacaoPadrao, List<Regra> regras) {

    public RegraRoteamento {
        regras = regras == null ? List.of() : List.copyOf(regras);
    }

    /** Usada quando o estabelecimento-service esta indisponivel e nao ha cache (degradacao controlada). */
    public static RegraRoteamento vazia() {
        return new RegraRoteamento(null, List.of());
    }

    /** Uma regra aponta para uma estacao por item especifico ou por categoria. */
    public record Regra(String itemCardapioId, String categoria, String estacaoId, int prioridade) {
    }
}
