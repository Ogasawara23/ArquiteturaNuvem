package br.com.smartkds.pedido.shared.dominio;

public abstract class DominioException extends RuntimeException {
    protected DominioException(String mensagem) {
        super(mensagem);
    }
}
