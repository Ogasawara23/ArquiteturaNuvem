package br.com.smartkds.pedido.shared.dominio;

/** Cancelamento recusado pela PoliticaCancelamento (409). */
public class ItemNaoCancelavelException extends DominioException {
    public ItemNaoCancelavelException(String mensagem) {
        super(mensagem);
    }
}
