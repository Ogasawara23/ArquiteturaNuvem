package br.com.smartkds.estabelecimento.shared.web;

/** Erro de validacao de entrada especifico de uma fatia (400). */
public class RequisicaoInvalidaException extends RuntimeException {
    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
