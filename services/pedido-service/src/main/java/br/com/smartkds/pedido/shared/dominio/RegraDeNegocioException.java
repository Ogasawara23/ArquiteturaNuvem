package br.com.smartkds.pedido.shared.dominio;

/** Entrada que viola uma regra do dominio (400). */
public class RegraDeNegocioException extends DominioException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
