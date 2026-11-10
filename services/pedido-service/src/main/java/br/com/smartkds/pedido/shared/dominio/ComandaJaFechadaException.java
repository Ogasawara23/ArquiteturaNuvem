package br.com.smartkds.pedido.shared.dominio;

/** Operacao em comanda que nao esta ABERTA (409). */
public class ComandaJaFechadaException extends DominioException {
    public ComandaJaFechadaException(String mensagem) {
        super(mensagem);
    }
}
