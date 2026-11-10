package br.com.smartkds.pedido.shared.dominio;

/** Ciclo de vida do item (Figura 22). */
public enum StatusItem {
    PENDENTE,
    CONFIRMADO,
    EM_PRODUCAO,
    PRONTO,
    ENTREGUE,
    CANCELADO
}
