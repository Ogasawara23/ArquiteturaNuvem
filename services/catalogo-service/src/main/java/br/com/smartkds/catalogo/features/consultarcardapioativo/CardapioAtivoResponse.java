package br.com.smartkds.catalogo.features.consultarcardapioativo;

import br.com.smartkds.catalogo.shared.dominio.ItemNaVersao;

import java.time.Instant;
import java.util.List;

record CardapioAtivoResponse(String versaoId, Instant publicadaEm, List<CategoriaDto> categorias) {

    record CategoriaDto(String nome, List<ItemNaVersao> itens) {
    }
}
