package br.com.smartkds.producao.shared.dominio;

import java.util.List;
import java.util.UUID;

/** Item recebido no PedidoConfirmado, ainda nao atribuido a uma estacao. */
public record ItemParaProduzir(UUID itemComandaId,
                               String itemCardapioId,
                               String categoria,
                               String descricao,
                               int quantidade,
                               List<String> modificadores,
                               String observacao,
                               String estacaoSugerida,
                               int tempoAlvoPreparoSegundos) {
    public ItemParaProduzir {
        modificadores = modificadores == null ? List.of() : List.copyOf(modificadores);
    }
}
