package br.com.smartkds.catalogo.features.publicarversaocardapio;

import java.time.Instant;

record PublicarVersaoCardapioResponse(String versaoId, Instant publicadaEm, int quantidadeDeItens) {
}
