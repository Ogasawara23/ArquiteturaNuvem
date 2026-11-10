package br.com.smartkds.catalogo.shared.dominio;

public abstract class DominioException extends RuntimeException {
    protected DominioException(String mensagem) {
        super(mensagem);
    }
}
