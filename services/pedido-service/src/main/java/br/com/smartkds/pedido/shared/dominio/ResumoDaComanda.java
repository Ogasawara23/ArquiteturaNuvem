package br.com.smartkds.pedido.shared.dominio;

public record ResumoDaComanda(Dinheiro subtotal, Dinheiro taxaServico, Dinheiro total, int quantidadeDeItens) {
}
