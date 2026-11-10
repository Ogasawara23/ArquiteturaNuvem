package br.com.smartkds.estabelecimento.features.listarestacoes;

import java.util.UUID;

record EstacaoDto(UUID id, String codigo, String nome, int ordemExibicao, int percentualAtencao, boolean ativa) {
}
