package br.com.smartkds.pedido.shared.persistencia;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Mapeamento ORM da tabela comanda. O dominio nunca ve esta classe. */
@Entity
@Table(name = "comanda")
public class ComandaEntity {

    @Id
    UUID id;
    String tenantId;
    String mesaId;
    String mesaRotulo;
    String versaoCardapioId;
    String status;
    Long subtotalCentavos;
    Long taxaServicoCentavos;
    Long totalCentavos;
    Instant abertaEm;
    Instant fechadaEm;

    @Version
    Long versaoOtimista;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "comanda_id", nullable = false)
    @OrderBy("adicionadoEm ASC")
    List<ItemComandaEntity> itens = new ArrayList<>();

    protected ComandaEntity() {
    }
}
