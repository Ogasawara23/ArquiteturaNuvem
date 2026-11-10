package br.com.smartkds.catalogo.features.alterardisponibilidade;

import jakarta.validation.constraints.NotNull;

record AlterarDisponibilidadeRequest(@NotNull Boolean disponivel) {
}
