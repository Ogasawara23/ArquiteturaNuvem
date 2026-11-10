package br.com.smartkds.catalogo.shared.dominio;

import java.util.List;

public record GrupoModificador(String nome,
                               boolean obrigatorio,
                               int minimoSelecoes,
                               int maximoSelecoes,
                               List<OpcaoModificador> opcoes) {
    public GrupoModificador {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("nome do grupo de modificadores e obrigatorio");
        }
        opcoes = opcoes == null ? List.of() : List.copyOf(opcoes);
        if (minimoSelecoes < 0 || maximoSelecoes < minimoSelecoes) {
            throw new RegraDeNegocioException("faixa de selecoes invalida no grupo " + nome);
        }
        if (obrigatorio && minimoSelecoes < 1) {
            throw new RegraDeNegocioException("grupo obrigatorio exige ao menos uma selecao: " + nome);
        }
        if (maximoSelecoes > opcoes.size()) {
            throw new RegraDeNegocioException("maximo de selecoes maior que o numero de opcoes no grupo " + nome);
        }
    }
}
