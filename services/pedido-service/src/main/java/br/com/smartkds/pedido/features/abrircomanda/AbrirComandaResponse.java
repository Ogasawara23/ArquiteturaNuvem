package br.com.smartkds.pedido.features.abrircomanda;

import java.time.Instant;
import java.util.UUID;

record AbrirComandaResponse(UUID comandaId, String versaoCardapioId, boolean reaproveitada, Instant abertaEm) {
}
