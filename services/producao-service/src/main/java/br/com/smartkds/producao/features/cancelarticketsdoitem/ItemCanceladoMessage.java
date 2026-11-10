package br.com.smartkds.producao.features.cancelarticketsdoitem;

import java.time.Instant;
import java.util.UUID;

/** Contrato consumido de smartkds.pedidos / pedido.item.cancelado. */
record ItemCanceladoMessage(UUID eventoId, String tenantId, UUID comandaId, UUID itemComandaId, String motivo,
                            Instant ocorridoEm) {
}
