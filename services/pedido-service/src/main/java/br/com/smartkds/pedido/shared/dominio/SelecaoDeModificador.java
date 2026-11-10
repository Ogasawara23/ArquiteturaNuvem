package br.com.smartkds.pedido.shared.dominio;

/** O que o cliente escolheu: apenas nomes. O preco vem do snapshot do cardapio. */
public record SelecaoDeModificador(String grupo, String opcao) {
}
