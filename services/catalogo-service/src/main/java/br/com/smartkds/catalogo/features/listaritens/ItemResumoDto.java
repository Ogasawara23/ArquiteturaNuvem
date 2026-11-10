package br.com.smartkds.catalogo.features.listaritens;

import java.time.Instant;

record ItemResumoDto(String id,
                     String nome,
                     String categoria,
                     long precoCentavos,
                     String estacaoSugerida,
                     int tempoAlvoPreparoSegundos,
                     boolean disponivel,
                     Instant atualizadoEm) {
}
