package br.com.smartkds.pedido.shared.dominio;

import java.util.List;

/**
 * Copia do item na versao ativa do cardapio, obtida uma unica vez na adicao (ADR-11).
 * A partir daqui o preco e a descricao ficam congelados na comanda.
 */
public record SnapshotItemCardapio(String itemCardapioId,
                                   String descricao,
                                   String categoria,
                                   Dinheiro precoUnitario,
                                   String estacaoSugerida,
                                   int tempoAlvoPreparoSegundos,
                                   List<GrupoDeOpcoes> grupos) {

    public SnapshotItemCardapio {
        grupos = grupos == null ? List.of() : List.copyOf(grupos);
    }

    public record GrupoDeOpcoes(String nome, boolean obrigatorio, int minimoSelecoes, int maximoSelecoes,
                                List<Opcao> opcoes) {
        public GrupoDeOpcoes {
            opcoes = opcoes == null ? List.of() : List.copyOf(opcoes);
        }
    }

    public record Opcao(String nome, Dinheiro precoAdicional, boolean disponivel) {
    }
}
