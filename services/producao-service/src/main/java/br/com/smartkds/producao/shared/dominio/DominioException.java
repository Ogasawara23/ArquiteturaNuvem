package br.com.smartkds.producao.shared.dominio;

public abstract class DominioException extends RuntimeException {
    protected DominioException(String mensagem) {
        super(mensagem);
    }
}
