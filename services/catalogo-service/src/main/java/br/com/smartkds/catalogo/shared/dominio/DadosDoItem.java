package br.com.smartkds.catalogo.shared.dominio;

import java.util.List;

/** Conjunto de atributos editaveis de um item, usado no cadastro e na atualizacao. */
public record DadosDoItem(String nome,
                          String descricao,
                          String categoria,
                          long precoCentavos,
                          String estacaoSugerida,
                          int tempoAlvoPreparoSegundos,
                          String imagemUrl,
                          List<GrupoModificador> gruposModificadores) {
}
