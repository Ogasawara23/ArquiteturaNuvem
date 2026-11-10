package br.com.smartkds.producao.features.listarticketsdaestacao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record TicketDaEstacaoDto(UUID ticketId,
                          UUID comandaId,
                          String mesa,
                          String estacaoId,
                          String status,
                          int tempoAlvoSegundos,
                          long decorridoSegundos,
                          String situacaoSla,
                          Instant criadoEm,
                          List<Item> itens) {

    record Item(UUID itemComandaId, String descricao, int quantidade, List<String> modificadores,
                String observacao, boolean cancelado) {
    }
}
