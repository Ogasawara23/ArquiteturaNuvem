package br.com.smartkds.estabelecimento.shared.dominio;

public abstract class DominioException extends RuntimeException {
    protected DominioException(String mensagem) {
        super(mensagem);
    }
}
