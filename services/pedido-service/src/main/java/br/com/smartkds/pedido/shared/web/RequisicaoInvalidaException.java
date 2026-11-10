package br.com.smartkds.pedido.shared.web;

/** Erro de validacao de entrada especifico de uma fatia (400). */
public class RequisicaoInvalidaException extends RuntimeException {
    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
