package br.com.smartkds.pedido.shared.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Agregado raiz do contexto de Pedido. Invariantes garantidas aqui, nunca nas fatias:
 * 1. Itens so entram em comanda ABERTA;
 * 2. Preco e descricao sao congelados no momento da adicao;
 * 3. Itens cancelados nao entram no subtotal;
 * 4. Comanda FECHADA e imutavel.
 */
public class Comanda {

    private static final int QUANTIDADE_MAXIMA = 99;

    private final ComandaId id;
    private final TenantId tenantId;
    private final MesaId mesaId;
    private final String mesaRotulo;
    private final String versaoCardapioId;
    private final List<ItemComanda> itens;
    private final Instant abertaEm;
    private final Long versao;
    private StatusComanda status;
    private Instant fechadaEm;
    private ResumoDaComanda resumo;

    @SuppressWarnings("java:S107")
    private Comanda(ComandaId id, TenantId tenantId, MesaId mesaId, String mesaRotulo, String versaoCardapioId,
                    StatusComanda status, List<ItemComanda> itens, Instant abertaEm, Instant fechadaEm,
                    ResumoDaComanda resumo, Long versao) {
        this.id = id;
        this.tenantId = tenantId;
        this.mesaId = mesaId;
        this.mesaRotulo = mesaRotulo;
        this.versaoCardapioId = versaoCardapioId;
        this.status = status;
        this.itens = new ArrayList<>(itens);
        this.abertaEm = abertaEm;
        this.fechadaEm = fechadaEm;
        this.resumo = resumo;
        this.versao = versao;
    }

    public static Comanda abrir(TenantId tenantId, MesaId mesaId, String mesaRotulo,
                                String versaoCardapioId, Instant agora) {
        if (versaoCardapioId == null || versaoCardapioId.isBlank()) {
            throw new RegraDeNegocioException("o estabelecimento nao possui cardapio publicado");
        }
        String rotulo = mesaRotulo == null || mesaRotulo.isBlank() ? mesaId.valor() : mesaRotulo;
        return new Comanda(ComandaId.novo(), tenantId, mesaId, rotulo, versaoCardapioId,
                StatusComanda.ABERTA, List.of(), agora, null, null, null);
    }

    /** Reconstitui o agregado a partir da persistencia. */
    @SuppressWarnings("java:S107")
    public static Comanda restaurar(ComandaId id, TenantId tenantId, MesaId mesaId, String mesaRotulo,
                                    String versaoCardapioId, StatusComanda status, List<ItemComanda> itens,
                                    Instant abertaEm, Instant fechadaEm, ResumoDaComanda resumo, Long versao) {
        return new Comanda(id, tenantId, mesaId, mesaRotulo, versaoCardapioId, status, itens,
                abertaEm, fechadaEm, resumo, versao);
    }

    public ItemComanda adicionarItem(SnapshotItemCardapio snapshot, int quantidade,
                                     List<SelecaoDeModificador> selecoes, String observacao, Instant agora) {
        exigirStatus(StatusComanda.ABERTA);
        validarQuantidadeEObservacao(quantidade, observacao);
        List<ModificadorSelecionado> modificadores = resolverModificadores(snapshot,
                selecoes == null ? List.of() : selecoes);
        ItemComanda item = ItemComanda.novo(snapshot, quantidade, modificadores, observacao, agora);
        itens.add(item);
        return item;
    }

    private static void validarQuantidadeEObservacao(int quantidade, String observacao) {
        if (quantidade < 1 || quantidade > QUANTIDADE_MAXIMA) {
            throw new RegraDeNegocioException("quantidade deve estar entre 1 e " + QUANTIDADE_MAXIMA);
        }
        if (observacao != null && observacao.length() > 200) {
            throw new RegraDeNegocioException("observacao excede 200 caracteres");
        }
    }

    private static List<ModificadorSelecionado> resolverModificadores(SnapshotItemCardapio snapshot,
                                                                      List<SelecaoDeModificador> selecoes) {
        List<ModificadorSelecionado> resultado = new ArrayList<>();
        for (SelecaoDeModificador selecao : selecoes) {
            var grupo = snapshot.grupos().stream()
                    .filter(g -> g.nome().equalsIgnoreCase(selecao.grupo()))
                    .findFirst()
                    .orElseThrow(() -> new RegraDeNegocioException("grupo de modificadores inexistente: " + selecao.grupo()));
            var opcao = grupo.opcoes().stream()
                    .filter(o -> o.nome().equalsIgnoreCase(selecao.opcao()))
                    .findFirst()
                    .orElseThrow(() -> new RegraDeNegocioException("opcao inexistente: " + selecao.opcao()));
            if (!opcao.disponivel()) {
                throw new RegraDeNegocioException("opcao indisponivel: " + opcao.nome());
            }
            resultado.add(new ModificadorSelecionado(grupo.nome(), opcao.nome(), opcao.precoAdicional()));
        }
        Map<String, Long> porGrupo = resultado.stream()
                .collect(Collectors.groupingBy(ModificadorSelecionado::grupo, Collectors.counting()));
        for (var grupo : snapshot.grupos()) {
            long selecionadas = porGrupo.getOrDefault(grupo.nome(), 0L);
            if (selecionadas < grupo.minimoSelecoes() || selecionadas > grupo.maximoSelecoes()) {
                throw new RegraDeNegocioException("o grupo '" + grupo.nome() + "' exige entre "
                        + grupo.minimoSelecoes() + " e " + grupo.maximoSelecoes() + " selecoes");
            }
        }
        return resultado;
    }

    public ResultadoConfirmacao confirmarItensPendentes(String chaveIdempotencia, Instant agora) {
        exigirStatus(StatusComanda.ABERTA);
        List<ItemComanda> jaConfirmadosComAChave = itens.stream()
                .filter(i -> chaveIdempotencia != null && chaveIdempotencia.equals(i.chaveConfirmacao()))
                .toList();
        if (!jaConfirmadosComAChave.isEmpty()) {
            return new ResultadoConfirmacao(jaConfirmadosComAChave, true);
        }
        List<ItemComanda> pendentes = itens.stream().filter(i -> i.status() == StatusItem.PENDENTE).toList();
        if (pendentes.isEmpty()) {
            throw new TransicaoInvalidaException("nao ha itens pendentes para confirmar");
        }
        pendentes.forEach(item -> item.confirmar(chaveIdempotencia, agora));
        return new ResultadoConfirmacao(pendentes, false);
    }

    /** Update do CRUD: altera quantidade e observacao de um item ainda no carrinho. */
    public ItemComanda alterarItem(ItemComandaId itemId, int quantidade, String observacao) {
        exigirStatus(StatusComanda.ABERTA);
        validarQuantidadeEObservacao(quantidade, observacao);
        ItemComanda item = item(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("ItemComanda", itemId.valor()));
        item.alterar(quantidade, observacao);
        return item;
    }

    /**
     * Delete do CRUD: cancela a comanda inteira. Recusado se algum item ja entrou em producao.
     * Devolve os itens que ja estavam na cozinha, para que a fatia publique a compensacao.
     */
    public List<ItemComanda> cancelar(String motivo, Instant agora) {
        exigirStatus(StatusComanda.ABERTA);
        boolean produzido = itens.stream().anyMatch(i -> i.status() == StatusItem.EM_PRODUCAO
                || i.status() == StatusItem.PRONTO || i.status() == StatusItem.ENTREGUE);
        if (produzido) {
            throw new TransicaoInvalidaException("a comanda possui itens ja produzidos; feche a conta em vez de cancelar");
        }
        List<ItemComanda> naCozinha = new ArrayList<>();
        for (ItemComanda item : itens) {
            if (item.status() == StatusItem.CANCELADO) {
                continue;
            }
            if (item.foiEnviadoParaCozinha()) {
                naCozinha.add(item);
            }
            item.cancelar(motivo == null || motivo.isBlank() ? "comanda cancelada" : motivo, agora);
        }
        this.status = StatusComanda.CANCELADA;
        this.fechadaEm = agora;
        return naCozinha;
    }

    public ItemComanda cancelarItem(ItemComandaId itemId, String motivo, Instant agora) {
        exigirStatus(StatusComanda.ABERTA);
        ItemComanda item = item(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("ItemComanda", itemId.valor()));
        item.cancelar(motivo, agora);
        return item;
    }

    /** Evento TicketStatusAlterado (EM_PREPARO). Tolerante a reentrega e a ordem. */
    public int registrarInicioDaProducao(Collection<ItemComandaId> ids) {
        return aplicarSeAberta(ids, ItemComanda::iniciarProducao);
    }

    /** Evento TicketStatusAlterado (PRONTO). */
    public int registrarItensProntos(Collection<ItemComandaId> ids) {
        return aplicarSeAberta(ids, ItemComanda::marcarPronto);
    }

    /** Evento TicketExpedido. */
    public int marcarItensComoEntregues(Collection<ItemComandaId> ids, Instant agora) {
        return aplicarSeAberta(ids, item -> item.marcarEntregue(agora));
    }

    private int aplicarSeAberta(Collection<ItemComandaId> ids, Predicate<ItemComanda> transicao) {
        if (status != StatusComanda.ABERTA) {
            return 0; // comanda FECHADA e imutavel: o evento tardio e apenas descartado
        }
        Set<ItemComandaId> alvo = new HashSet<>(ids);
        int alterados = 0;
        for (ItemComanda item : itens) {
            if (alvo.contains(item.id()) && transicao.test(item)) {
                alterados++;
            }
        }
        return alterados;
    }

    public ResumoDaComanda fechar(BigDecimal percentualTaxaServico, Instant agora) {
        exigirStatus(StatusComanda.ABERTA);
        if (possuiItensPendentes()) {
            throw new TransicaoInvalidaException("existem itens pendentes de confirmacao");
        }
        BigDecimal taxa = percentualTaxaServico == null ? BigDecimal.ZERO : percentualTaxaServico;
        if (taxa.compareTo(BigDecimal.valueOf(30)) > 0) {
            throw new RegraDeNegocioException("taxa de servico acima de 30%");
        }
        Dinheiro subtotal = subtotal();
        Dinheiro valorTaxa = subtotal.percentual(taxa);
        this.resumo = new ResumoDaComanda(subtotal, valorTaxa, subtotal.somar(valorTaxa), itensFaturaveis().size());
        this.status = StatusComanda.FECHADA;
        this.fechadaEm = agora;
        return resumo;
    }

    public Dinheiro subtotal() {
        return itensFaturaveis().stream().map(ItemComanda::total).reduce(Dinheiro.zero(), Dinheiro::somar);
    }

    public boolean possuiItensPendentes() {
        return itens.stream().anyMatch(i -> i.status() == StatusItem.PENDENTE);
    }

    public Optional<ItemComanda> item(ItemComandaId itemId) {
        return itens.stream().filter(i -> i.id().equals(itemId)).findFirst();
    }

    private List<ItemComanda> itensFaturaveis() {
        return itens.stream().filter(i -> i.status() != StatusItem.CANCELADO).toList();
    }

    private void exigirStatus(StatusComanda esperado) {
        if (status != esperado) {
            throw new ComandaJaFechadaException("comanda em status " + status);
        }
    }

    public ComandaId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public MesaId mesaId() { return mesaId; }
    public String mesaRotulo() { return mesaRotulo; }
    public String versaoCardapioId() { return versaoCardapioId; }
    public StatusComanda status() { return status; }
    public List<ItemComanda> itens() { return Collections.unmodifiableList(itens); }
    public Instant abertaEm() { return abertaEm; }
    public Instant fechadaEm() { return fechadaEm; }
    public Optional<ResumoDaComanda> resumo() { return Optional.ofNullable(resumo); }
    public Long versao() { return versao; }
}
