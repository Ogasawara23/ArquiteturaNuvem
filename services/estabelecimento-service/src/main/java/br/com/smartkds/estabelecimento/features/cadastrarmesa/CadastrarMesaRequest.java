package br.com.smartkds.estabelecimento.features.cadastrarmesa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CadastrarMesaRequest(@NotBlank @Size(max = 80) String rotulo) {
}
