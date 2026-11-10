package br.com.smartkds.pedido.shared.dominio;

/**
 * Servico de dominio: o cliente so pode cancelar o que ainda nao entrou em producao.
 * A partir de EM_PRODUCAO o insumo ja foi consumido (Figura 22).
 */
public final class PoliticaCancelamento {

    private PoliticaCancelamento() { }

    public static boolean permiteCancelar(ItemComanda item) {
        return item.status() == StatusItem.PENDENTE || item.status() == StatusItem.CONFIRMADO;
    }

    public static String motivoDaRecusa(ItemComanda item) {
        return switch (item.status()) {
            case EM_PRODUCAO, PRONTO -> "item ja esta em producao";
            case ENTREGUE -> "item ja foi entregue";
            case CANCELADO -> "item ja esta cancelado";
            default -> "cancelamento permitido";
        };
    }
}
