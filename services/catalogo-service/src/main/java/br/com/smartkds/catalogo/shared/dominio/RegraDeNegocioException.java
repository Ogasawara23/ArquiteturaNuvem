package br.com.smartkds.catalogo.shared.dominio;

/** Violacao de invariante de entrada do dominio (convertida em 400 pelo ManipuladorDeErros). */
public class RegraDeNegocioException extends DominioException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
