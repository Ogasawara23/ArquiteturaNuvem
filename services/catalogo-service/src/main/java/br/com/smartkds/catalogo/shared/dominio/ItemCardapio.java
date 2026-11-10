package br.com.smartkds.catalogo.shared.dominio;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Item editavel do catalogo. O cliente nunca le este objeto diretamente:
 * ele le a {@link VersaoCardapio} publicada, que e imutavel.
 */
public class ItemCardapio {

    private final String id;
    private final TenantId tenantId;
    private String nome;
    private String descricao;
    private String categoria;
    private long precoCentavos;
    private String estacaoSugerida;
    private int tempoAlvoPreparoSegundos;
    private boolean disponivel;
    private String imagemUrl;
    private List<GrupoModificador> gruposModificadores;
    private Instant atualizadoEm;

    private ItemCardapio(String id, TenantId tenantId, DadosDoItem dados, boolean disponivel, Instant atualizadoEm) {
        this.id = id;
        this.tenantId = tenantId;
        this.disponivel = disponivel;
        aplicar(dados, atualizadoEm);
    }

    public static ItemCardapio cadastrar(TenantId tenantId, DadosDoItem dados, Instant agora) {
        return new ItemCardapio(UUID.randomUUID().toString(), tenantId, dados, true, agora);
    }

    /** Reconstitui o agregado a partir da persistencia. */
    public static ItemCardapio restaurar(String id, TenantId tenantId, DadosDoItem dados,
                                         boolean disponivel, Instant atualizadoEm) {
        return new ItemCardapio(id, tenantId, dados, disponivel, atualizadoEm);
    }

    public void atualizar(DadosDoItem dados, Instant agora) {
        aplicar(dados, agora);
    }

    public void alterarDisponibilidade(boolean disponivel, Instant agora) {
        this.disponivel = disponivel;
        this.atualizadoEm = agora;
    }

    private void aplicar(DadosDoItem dados, Instant agora) {
        if (dados == null) {
            throw new RegraDeNegocioException("dados do item sao obrigatorios");
        }
        exigirTexto(dados.nome(), "nome");
        exigirTexto(dados.categoria(), "categoria");
        exigirTexto(dados.estacaoSugerida(), "estacaoSugerida");
        if (dados.precoCentavos() <= 0) {
            throw new RegraDeNegocioException("preco deve ser maior que zero");
        }
        if (dados.tempoAlvoPreparoSegundos() <= 0) {
            throw new RegraDeNegocioException("tempo-alvo de preparo deve ser maior que zero");
        }
        this.nome = dados.nome().trim();
        this.descricao = dados.descricao();
        this.categoria = dados.categoria().trim();
        this.precoCentavos = dados.precoCentavos();
        this.estacaoSugerida = dados.estacaoSugerida().trim().toLowerCase();
        this.tempoAlvoPreparoSegundos = dados.tempoAlvoPreparoSegundos();
        this.imagemUrl = dados.imagemUrl();
        this.gruposModificadores = dados.gruposModificadores() == null
                ? List.of() : List.copyOf(dados.gruposModificadores());
        this.atualizadoEm = agora;
    }

    private static void exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeNegocioException(campo + " e obrigatorio");
        }
    }

    /** Copia imutavel usada na publicacao de uma versao. */
    public ItemNaVersao congelar() {
        return new ItemNaVersao(id, nome, descricao, categoria, precoCentavos,
                estacaoSugerida, tempoAlvoPreparoSegundos, imagemUrl, gruposModificadores);
    }

    public DadosDoItem dados() {
        return new DadosDoItem(nome, descricao, categoria, precoCentavos, estacaoSugerida,
                tempoAlvoPreparoSegundos, imagemUrl, gruposModificadores);
    }

    public String id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public String nome() { return nome; }
    public String descricao() { return descricao; }
    public String categoria() { return categoria; }
    public long precoCentavos() { return precoCentavos; }
    public String estacaoSugerida() { return estacaoSugerida; }
    public int tempoAlvoPreparoSegundos() { return tempoAlvoPreparoSegundos; }
    public boolean disponivel() { return disponivel; }
    public String imagemUrl() { return imagemUrl; }
    public List<GrupoModificador> gruposModificadores() { return gruposModificadores; }
    public Instant atualizadoEm() { return atualizadoEm; }
}
