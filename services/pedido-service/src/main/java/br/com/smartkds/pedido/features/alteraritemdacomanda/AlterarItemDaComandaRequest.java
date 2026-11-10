package br.com.smartkds.pedido.features.alteraritemdacomanda;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

record AlterarItemDaComandaRequest(@Min(1) @Max(99) int quantidade, @Size(max = 200) String observacao) {
}
