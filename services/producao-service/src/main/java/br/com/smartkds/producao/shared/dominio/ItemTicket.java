package br.com.smartkds.producao.shared.dominio;

import java.util.List;
import java.util.UUID;

public class ItemTicket {

    private final UUID id;
    private final UUID itemComandaId;
    private final String descricao;
    private final int quantidade;
    private final List<String> modificadores;
    private final String observacao;
    private boolean cancelado;

    public ItemTicket(UUID id, UUID itemComandaId, String descricao, int quantidade, List<String> modificadores,
                      String observacao, boolean cancelado) {
        this.id = id;
        this.itemComandaId = itemComandaId;
        this.descricao = descricao;
        this.quantidade = quantidade;
        this.modificadores = modificadores == null ? List.of() : List.copyOf(modificadores);
        this.observacao = observacao;
        this.cancelado = cancelado;
    }

    static ItemTicket de(ItemParaProduzir item) {
        return new ItemTicket(UUID.randomUUID(), item.itemComandaId(), item.descricao(), item.quantidade(),
                item.modificadores(), item.observacao(), false);
    }

    void cancelar() {
        cancelado = true;
    }

    public UUID id() { return id; }
    public UUID itemComandaId() { return itemComandaId; }
    public String descricao() { return descricao; }
    public int quantidade() { return quantidade; }
    public List<String> modificadores() { return modificadores; }
    public String observacao() { return observacao; }
    public boolean cancelado() { return cancelado; }
}
