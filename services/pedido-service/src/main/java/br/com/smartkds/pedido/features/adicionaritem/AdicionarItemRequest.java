package br.com.smartkds.pedido.features.adicionaritem;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

record AdicionarItemRequest(@NotBlank String itemCardapioId,
                            @Min(1) @Max(99) int quantidade,
                            List<ModificadorDto> modificadores,
                            @Size(max = 200) String observacao) {

    record ModificadorDto(String grupo, String opcao) {
    }
}
