package br.com.smartkds.pedido.features.abrircomanda;

import jakarta.validation.constraints.NotBlank;

record AbrirComandaRequest(@NotBlank String mesaId, String mesaRotulo) {
}
