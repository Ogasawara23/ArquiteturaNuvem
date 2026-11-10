package br.com.smartkds.estabelecimento.features.cadastrarestacao;

import jakarta.validation.constraints.NotBlank;

record CadastrarEstacaoRequest(String codigo, @NotBlank String nome, Integer ordemExibicao, Integer percentualAtencao) {
}
