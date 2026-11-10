package br.com.smartkds.catalogo.shared.dominio;

/** Publicacao de versao do cardapio que viola as regras (convertida em 422). */
public class VersaoInvalidaException extends DominioException {
    public VersaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
