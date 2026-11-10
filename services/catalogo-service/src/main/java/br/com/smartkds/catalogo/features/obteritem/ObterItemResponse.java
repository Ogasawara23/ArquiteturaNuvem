package br.com.smartkds.catalogo.features.obteritem;

import br.com.smartkds.catalogo.shared.dominio.GrupoModificador;

import java.time.Instant;
import java.util.List;

record ObterItemResponse(String id,
                         String nome,
                         String descricao,
                         String categoria,
                         long precoCentavos,
                         String estacaoSugerida,
                         int tempoAlvoPreparoSegundos,
                         boolean disponivel,
                         String imagemUrl,
                         List<GrupoModificador> gruposModificadores,
                         Instant atualizadoEm) {
}
