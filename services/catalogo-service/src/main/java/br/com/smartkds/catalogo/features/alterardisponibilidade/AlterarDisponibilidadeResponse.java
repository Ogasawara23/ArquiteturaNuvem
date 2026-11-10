package br.com.smartkds.catalogo.features.alterardisponibilidade;

import java.time.Instant;

record AlterarDisponibilidadeResponse(String id, boolean disponivel, Instant atualizadoEm) {
}
