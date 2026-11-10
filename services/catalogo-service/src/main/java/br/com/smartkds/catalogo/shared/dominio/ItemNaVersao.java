package br.com.smartkds.catalogo.shared.dominio;

import java.util.List;

/** Copia imutavel de um item no instante da publicacao da versao. */
public record ItemNaVersao(String itemCardapioId,
                           String nome,
                           String descricao,
                           String categoria,
                           long precoCentavos,
                           String estacaoSugerida,
                           int tempoAlvoPreparoSegundos,
                           String imagemUrl,
                           List<GrupoModificador> gruposModificadores) {
    public ItemNaVersao {
        gruposModificadores = gruposModificadores == null ? List.of() : List.copyOf(gruposModificadores);
    }
}
