package br.com.smartkds.pedido.shared.persistencia;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "item_comanda")
public class ItemComandaEntity {

    @Id
    UUID id;
    String itemCardapioId;
    String descricao;
    String categoria;
    long precoUnitarioCentavos;
    int quantidade;
    String observacao;
    String estacaoSugerida;
    int tempoAlvoPreparoSegundos;
    String status;
    String chaveConfirmacao;
    String motivoCancelamento;
    Instant adicionadoEm;
    Instant confirmadoEm;
    Instant entregueEm;
    Instant canceladoEm;

    @ElementCollection
    @CollectionTable(name = "modificador_item", joinColumns = @JoinColumn(name = "item_comanda_id"))
    List<Modificador> modificadores = new ArrayList<>();

    protected ItemComandaEntity() {
    }

    @Embeddable
    public static class Modificador {
        String grupo;
        String opcao;
        long precoAdicionalCentavos;

        protected Modificador() {
        }

        Modificador(String grupo, String opcao, long precoAdicionalCentavos) {
            this.grupo = grupo;
            this.opcao = opcao;
            this.precoAdicionalCentavos = precoAdicionalCentavos;
        }
    }
}
