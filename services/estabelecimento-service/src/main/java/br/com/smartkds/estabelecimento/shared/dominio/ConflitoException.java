package br.com.smartkds.estabelecimento.shared.dominio;

/** Recurso duplicado no tenant (409). */
public class ConflitoException extends DominioException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
