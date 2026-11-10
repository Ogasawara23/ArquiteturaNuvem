package br.com.smartkds.catalogo.features.cadastraritem;

import br.com.smartkds.catalogo.shared.dominio.GrupoModificador;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

record CadastrarItemRequest(@NotBlank String nome,
                            String descricao,
                            @NotBlank String categoria,
                            @Positive long precoCentavos,
                            @NotBlank String estacaoSugerida,
                            @Positive int tempoAlvoPreparoSegundos,
                            String imagemUrl,
                            List<GrupoModificador> gruposModificadores) {
}
