package br.com.smartkds.producao.shared.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agregado raiz do contexto de Producao: "o que esta estacao precisa produzir".
 * Uma comanda gera um ticket por estacao (Secao 8.5).
 */
public class TicketProducao {

    private final UUID id;
    private final TenantId tenantId;
    private final UUID comandaId;
    private final String mesaRotulo;
    private final String estacaoId;
    private final int tempoAlvoSegundos;
    private final List<ItemTicket> itens;
    private final Instant criadoEm;
    private final Long versao;
    private StatusTicket status;
    private Instant iniciadoEm;
    private Instant prontoEm;
    private Instant expedidoEm;
    private Instant canceladoEm;

    @SuppressWarnings("java:S107")
    private TicketProducao(UUID id, TenantId tenantId, UUID comandaId, String mesaRotulo, String estacaoId,
                           int tempoAlvoSegundos, List<ItemTicket> itens, StatusTicket status, Instant criadoEm,
                           Instant iniciadoEm, Instant prontoEm, Instant expedidoEm, Instant canceladoEm,
                           Long versao) {
        this.id = id;
        this.tenantId = tenantId;
        this.comandaId = comandaId;
        this.mesaRotulo = mesaRotulo;
        this.estacaoId = estacaoId;
        this.tempoAlvoSegundos = tempoAlvoSegundos;
        this.itens = new ArrayList<>(itens);
        this.status = status;
        this.criadoEm = criadoEm;
        this.iniciadoEm = iniciadoEm;
        this.prontoEm = prontoEm;
        this.expedidoEm = expedidoEm;
        this.canceladoEm = canceladoEm;
        this.versao = versao;
    }

    public static TicketProducao criar(TenantId tenantId, UUID comandaId, String mesaRotulo, String estacaoId,
                                       List<ItemParaProduzir> itens, Instant agora) {
        if (itens == null || itens.isEmpty()) {
            throw new RegraDeNegocioException("ticket sem itens");
        }
        int tempoAlvo = itens.stream().mapToInt(ItemParaProduzir::tempoAlvoPreparoSegundos).max().orElse(600);
        return new TicketProducao(UUID.randomUUID(), tenantId, comandaId,
                mesaRotulo == null ? "Mesa" : mesaRotulo, estacaoId, Math.max(tempoAlvo, 1),
                itens.stream().map(ItemTicket::de).toList(), StatusTicket.RECEBIDO, agora,
                null, null, null, null, null);
    }

    @SuppressWarnings("java:S107")
    public static TicketProducao restaurar(UUID id, TenantId tenantId, UUID comandaId, String mesaRotulo,
                                           String estacaoId, int tempoAlvoSegundos, List<ItemTicket> itens,
                                           StatusTicket status, Instant criadoEm, Instant iniciadoEm,
                                           Instant prontoEm, Instant expedidoEm, Instant canceladoEm, Long versao) {
        return new TicketProducao(id, tenantId, comandaId, mesaRotulo, estacaoId, tempoAlvoSegundos, itens,
                status, criadoEm, iniciadoEm, prontoEm, expedidoEm, canceladoEm, versao);
    }

    /** RECEBIDO -> EM_PREPARO -> PRONTO. Qualquer outra transicao e invalida (409). */
    public void avancarStatus(StatusTicket novo, Instant agora) {
        if (status == StatusTicket.RECEBIDO && novo == StatusTicket.EM_PREPARO) {
            status = StatusTicket.EM_PREPARO;
            iniciadoEm = agora;
        } else if (status == StatusTicket.EM_PREPARO && novo == StatusTicket.PRONTO) {
            status = StatusTicket.PRONTO;
            prontoEm = agora;
        } else {
            throw new TransicaoInvalidaException("transicao de " + status + " para " + novo + " nao permitida");
        }
    }

    /** Bump: so um ticket PRONTO e expedido. */
    public void expedir(Instant agora) {
        if (status != StatusTicket.PRONTO) {
            throw new TransicaoInvalidaException("so e possivel expedir ticket PRONTO; status atual " + status);
        }
        status = StatusTicket.EXPEDIDO;
        expedidoEm = agora;
    }

    /**
     * Compensacao da saga (Figura 19). Retorna true se algo mudou. Se o ticket ficar sem
     * itens ativos enquanto ainda nao saiu da cozinha, o ticket inteiro e cancelado.
     */
    public boolean cancelarItem(UUID itemComandaId, Instant agora) {
        boolean alterou = false;
        for (ItemTicket item : itens) {
            if (item.itemComandaId().equals(itemComandaId) && !item.cancelado()) {
                item.cancelar();
                alterou = true;
            }
        }
        if (alterou && semItensAtivos() && (status == StatusTicket.RECEBIDO || status == StatusTicket.EM_PREPARO)) {
            status = StatusTicket.CANCELADO;
            canceladoEm = agora;
        }
        return alterou;
    }

    public boolean semItensAtivos() {
        return itens.stream().allMatch(ItemTicket::cancelado);
    }

    public Duration decorrido(Instant agora) {
        Instant fim = switch (status) {
            case PRONTO -> prontoEm;
            case EXPEDIDO -> prontoEm != null ? prontoEm : expedidoEm;
            case CANCELADO -> canceladoEm;
            default -> agora;
        };
        return Duration.between(criadoEm, fim == null ? agora : fim);
    }

    public SituacaoSla situacaoSla(Instant agora, int percentualAtencao) {
        return new JanelaDeSla(tempoAlvoSegundos, percentualAtencao).classificar(decorrido(agora));
    }

    public boolean estaAtrasado(Instant agora) {
        return situacaoSla(agora, JanelaDeSla.PERCENTUAL_ATENCAO_PADRAO) == SituacaoSla.ATRASADO;
    }

    public List<UUID> itensAtivosDaComanda() {
        return itens.stream().filter(i -> !i.cancelado()).map(ItemTicket::itemComandaId).toList();
    }

    public UUID id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public UUID comandaId() { return comandaId; }
    public String mesaRotulo() { return mesaRotulo; }
    public String estacaoId() { return estacaoId; }
    public int tempoAlvoSegundos() { return tempoAlvoSegundos; }
    public List<ItemTicket> itens() { return Collections.unmodifiableList(itens); }
    public StatusTicket status() { return status; }
    public Instant criadoEm() { return criadoEm; }
    public Instant iniciadoEm() { return iniciadoEm; }
    public Instant prontoEm() { return prontoEm; }
    public Instant expedidoEm() { return expedidoEm; }
    public Instant canceladoEm() { return canceladoEm; }
    public Long versao() { return versao; }
}
