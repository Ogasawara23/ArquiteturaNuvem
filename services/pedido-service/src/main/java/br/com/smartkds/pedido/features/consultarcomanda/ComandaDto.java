package br.com.smartkds.pedido.features.consultarcomanda;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record ComandaDto(UUID comandaId,
                  String mesaId,
                  String mesaRotulo,
                  String status,
                  String versaoCardapioId,
                  long subtotalCentavos,
                  Long taxaServicoCentavos,
                  Long totalCentavos,
                  Instant abertaEm,
                  Instant fechadaEm,
                  List<ItemDto> itens) {

    record ItemDto(UUID itemId,
                   String itemCardapioId,
                   String descricao,
                   int quantidade,
                   long precoUnitarioCentavos,
                   long totalCentavos,
                   List<String> modificadores,
                   String observacao,
                   String estacaoSugerida,
                   String status,
                   boolean podeSerCancelado,
                   Instant adicionadoEm,
                   Instant confirmadoEm,
                   Instant entregueEm) {
    }
}
