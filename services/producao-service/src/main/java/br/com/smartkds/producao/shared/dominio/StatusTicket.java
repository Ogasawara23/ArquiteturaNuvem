package br.com.smartkds.producao.shared.dominio;

/** Ciclo de vida do ticket (Figura 23). */
public enum StatusTicket {
    RECEBIDO,
    EM_PREPARO,
    PRONTO,
    EXPEDIDO,
    CANCELADO;

    public boolean ativo() {
        return this == RECEBIDO || this == EM_PREPARO || this == PRONTO;
    }
}
