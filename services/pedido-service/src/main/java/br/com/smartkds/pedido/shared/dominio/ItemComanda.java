package br.com.smartkds.pedido.shared.dominio;

import java.time.Instant;
import java.util.List;

/** Entidade interna do agregado Comanda. Preco e descricao sao congelados na criacao. */
public class ItemComanda {

    private final ItemComandaId id;
    private final String itemCardapioId;
    private final String descricao;
    private final String categoria;
    private final Dinheiro precoUnitario;
    private int quantidade;
    private final List<ModificadorSelecionado> modificadores;
    private String observacao;
    private final String estacaoSugerida;
    private final int tempoAlvoPreparoSegundos;
    private final Instant adicionadoEm;
    private StatusItem status;
    private String chaveConfirmacao;
    private String motivoCancelamento;
    private Instant confirmadoEm;
    private Instant entregueEm;
    private Instant canceladoEm;

    @SuppressWarnings("java:S107")
    public ItemComanda(ItemComandaId id, String itemCardapioId, String descricao, String categoria, Dinheiro precoUnitario,
                       int quantidade, List<ModificadorSelecionado> modificadores, String observacao,
                       String estacaoSugerida, int tempoAlvoPreparoSegundos, StatusItem status,
                       String chaveConfirmacao, String motivoCancelamento, Instant adicionadoEm,
                       Instant confirmadoEm, Instant entregueEm, Instant canceladoEm) {
        this.id = id;
        this.itemCardapioId = itemCardapioId;
        this.descricao = descricao;
        this.categoria = categoria;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
        this.modificadores = modificadores == null ? List.of() : List.copyOf(modificadores);
        this.observacao = observacao;
        this.estacaoSugerida = estacaoSugerida;
        this.tempoAlvoPreparoSegundos = tempoAlvoPreparoSegundos;
        this.status = status;
        this.chaveConfirmacao = chaveConfirmacao;
        this.motivoCancelamento = motivoCancelamento;
        this.adicionadoEm = adicionadoEm;
        this.confirmadoEm = confirmadoEm;
        this.entregueEm = entregueEm;
        this.canceladoEm = canceladoEm;
    }

    static ItemComanda novo(SnapshotItemCardapio snapshot, int quantidade,
                            List<ModificadorSelecionado> modificadores, String observacao, Instant agora) {
        return new ItemComanda(ItemComandaId.novo(), snapshot.itemCardapioId(), snapshot.descricao(), snapshot.categoria(),
                snapshot.precoUnitario(), quantidade, modificadores, observacao, snapshot.estacaoSugerida(),
                snapshot.tempoAlvoPreparoSegundos(), StatusItem.PENDENTE, null, null, agora, null, null, null);
    }

    void confirmar(String chave, Instant agora) {
        exigir(StatusItem.PENDENTE, "confirmar");
        this.status = StatusItem.CONFIRMADO;
        this.chaveConfirmacao = chave;
        this.confirmadoEm = agora;
    }

    /** So itens ainda no carrinho (PENDENTE) podem ser alterados. */
    void alterar(int novaQuantidade, String novaObservacao) {
        exigir(StatusItem.PENDENTE, "alterar");
        this.quantidade = novaQuantidade;
        this.observacao = novaObservacao;
    }

    boolean iniciarProducao() {
        if (status != StatusItem.CONFIRMADO) {
            return false;
        }
        status = StatusItem.EM_PRODUCAO;
        return true;
    }

    boolean marcarPronto() {
        if (status != StatusItem.CONFIRMADO && status != StatusItem.EM_PRODUCAO) {
            return false;
        }
        status = StatusItem.PRONTO;
        return true;
    }

    boolean marcarEntregue(Instant agora) {
        if (status != StatusItem.CONFIRMADO && status != StatusItem.EM_PRODUCAO && status != StatusItem.PRONTO) {
            return false;
        }
        status = StatusItem.ENTREGUE;
        entregueEm = agora;
        return true;
    }

    void cancelar(String motivo, Instant agora) {
        if (!PoliticaCancelamento.permiteCancelar(this)) {
            throw new ItemNaoCancelavelException(PoliticaCancelamento.motivoDaRecusa(this));
        }
        status = StatusItem.CANCELADO;
        motivoCancelamento = motivo == null || motivo.isBlank() ? "solicitado pelo cliente" : motivo;
        canceladoEm = agora;
    }

    private void exigir(StatusItem esperado, String operacao) {
        if (status != esperado) {
            throw new TransicaoInvalidaException("nao e possivel " + operacao + " item em " + status);
        }
    }

    public Dinheiro total() {
        Dinheiro unitarioComAdicionais = modificadores.stream()
                .map(ModificadorSelecionado::precoAdicional)
                .reduce(precoUnitario, Dinheiro::somar);
        return unitarioComAdicionais.multiplicar(quantidade);
    }

    public boolean podeSerCancelado() {
        return PoliticaCancelamento.permiteCancelar(this);
    }

    /** Um item que ja foi confirmado tem ticket na cozinha: cancelar exige compensacao. */
    public boolean foiEnviadoParaCozinha() {
        return confirmadoEm != null;
    }

    public List<String> modificadoresComoTexto() {
        return modificadores.stream().map(ModificadorSelecionado::opcao).toList();
    }

    public ItemComandaId id() { return id; }
    public String itemCardapioId() { return itemCardapioId; }
    public String descricao() { return descricao; }
    public String categoria() { return categoria; }
    public Dinheiro precoUnitario() { return precoUnitario; }
    public int quantidade() { return quantidade; }
    public List<ModificadorSelecionado> modificadores() { return modificadores; }
    public String observacao() { return observacao; }
    public String estacaoSugerida() { return estacaoSugerida; }
    public int tempoAlvoPreparoSegundos() { return tempoAlvoPreparoSegundos; }
    public StatusItem status() { return status; }
    public String chaveConfirmacao() { return chaveConfirmacao; }
    public String motivoCancelamento() { return motivoCancelamento; }
    public Instant adicionadoEm() { return adicionadoEm; }
    public Instant confirmadoEm() { return confirmadoEm; }
    public Instant entregueEm() { return entregueEm; }
    public Instant canceladoEm() { return canceladoEm; }
}
