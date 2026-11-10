package br.com.smartkds.pedido.shared.dominio;

/** Transicao de estado nao permitida (409). */
public class TransicaoInvalidaException extends DominioException {
    public TransicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
