package br.com.smartkds.pedido.shared.eventos;

import java.util.List;
import java.util.UUID;

/** Item denormalizado no evento: a producao nunca consulta o catalogo nem o pedido. */
public record ItemConfirmadoPayload(UUID itemComandaId,
                                    String itemCardapioId,
                                    String categoria,
                                    String descricao,
                                    int quantidade,
                                    List<String> modificadores,
                                    String observacao,
                                    String estacaoSugerida,
                                    int tempoAlvoPreparoSegundos) {
}
