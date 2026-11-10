package br.com.smartkds.catalogo.features.consultarversaocardapio;

import br.com.smartkds.catalogo.shared.dominio.ItemNaVersao;

import java.time.Instant;
import java.util.List;

record VersaoCardapioResponse(String versaoId,
                              Instant publicadaEm,
                              String publicadaPor,
                              int quantidadeDeItens,
                              List<ItemNaVersao> itens) {
}
