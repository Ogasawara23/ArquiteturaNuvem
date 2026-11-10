package br.com.smartkds.producao.shared.persistencia;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ticket_producao")
public class TicketEntity {

    @Id
    UUID id;
    String tenantId;
    UUID comandaId;
    String mesaRotulo;
    String estacaoId;
    String status;
    int tempoAlvoSegundos;
    Instant criadoEm;
    Instant iniciadoEm;
    Instant prontoEm;
    Instant expedidoEm;
    Instant canceladoEm;
    @Version
    Long versaoOtimista;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ticket_id", nullable = false)
    List<ItemTicketEntity> itens = new ArrayList<>();

    protected TicketEntity() {
    }

    @Entity
    @Table(name = "item_ticket")
    public static class ItemTicketEntity {
        @Id
        UUID id;
        UUID itemComandaId;
        String descricao;
        int quantidade;
        @Column(columnDefinition = "text")
        String modificadores;
        @Column(columnDefinition = "text")
        String observacao;
        boolean cancelado;

        protected ItemTicketEntity() {
        }
    }
}
