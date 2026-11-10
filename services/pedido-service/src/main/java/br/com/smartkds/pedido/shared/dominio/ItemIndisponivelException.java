package br.com.smartkds.pedido.shared.dominio;

/** Item inexistente ou esgotado na versao ativa do cardapio (409, Figura 14). */
public class ItemIndisponivelException extends DominioException {
    public ItemIndisponivelException(String itemCardapioId) {
        super("item indisponivel na versao do cardapio: " + itemCardapioId);
    }
}
