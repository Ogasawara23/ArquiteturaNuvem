package br.com.smartkds.producao.shared.dominio;

/** Transicao de estado nao permitida (409). */
public class TransicaoInvalidaException extends DominioException {
    public TransicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
