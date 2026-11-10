package br.com.smartkds.producao.dominio;

import br.com.smartkds.producao.shared.dominio.ItemParaProduzir;
import br.com.smartkds.producao.shared.dominio.JanelaDeSla;
import br.com.smartkds.producao.shared.dominio.RegraRoteamento;
import br.com.smartkds.producao.shared.dominio.RoteadorDeEstacao;
import br.com.smartkds.producao.shared.dominio.SituacaoSla;
import br.com.smartkds.producao.shared.dominio.StatusTicket;
import br.com.smartkds.producao.shared.dominio.TenantId;
import br.com.smartkds.producao.shared.dominio.TicketProducao;
import br.com.smartkds.producao.shared.dominio.TransicaoInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TicketProducaoTest {

    static final TenantId TENANT = new TenantId("rest-trattoria-centro");
    static final Instant AGORA = Instant.parse("2026-10-15T15:40:00Z");

    public static ItemParaProduzir item(String itemCardapioId, String categoria, String estacao, int tempoAlvo) {
        return new ItemParaProduzir(UUID.randomUUID(), itemCardapioId, categoria, "Item " + itemCardapioId, 1,
                List.of("Ao ponto"), null, estacao, tempoAlvo);
    }

    private static TicketProducao ticket(ItemParaProduzir... itens) {
        return TicketProducao.criar(TENANT, UUID.randomUUID(), "Mesa 12", "grill", List.of(itens), AGORA);
    }

    @Test
    void ciclo_completo_ate_o_bump() {
        TicketProducao t = ticket(item("hamb", "Lanches", "grill", 720));
        t.avancarStatus(StatusTicket.EM_PREPARO, AGORA.plusSeconds(10));
        t.avancarStatus(StatusTicket.PRONTO, AGORA.plusSeconds(600));
        t.expedir(AGORA.plusSeconds(620));

        assertThat(t.status()).isEqualTo(StatusTicket.EXPEDIDO);
        assertThat(t.decorrido(AGORA.plusSeconds(9999))).isEqualTo(Duration.ofSeconds(600));
    }

    @Test
    void transicoes_fora_de_ordem_sao_recusadas() {
        TicketProducao t = ticket(item("hamb", "Lanches", "grill", 720));
        assertThatThrownBy(() -> t.avancarStatus(StatusTicket.PRONTO, AGORA)).isInstanceOf(TransicaoInvalidaException.class);
        assertThatThrownBy(() -> t.expedir(AGORA)).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void sla_atencao_acima_de_70_por_cento_e_atraso_acima_do_alvo() {
        var janela = JanelaDeSla.padrao(600);
        assertThat(janela.classificar(Duration.ofSeconds(420))).isEqualTo(SituacaoSla.NO_PRAZO);
        assertThat(janela.classificar(Duration.ofSeconds(421))).isEqualTo(SituacaoSla.ATENCAO);
        assertThat(janela.classificar(Duration.ofSeconds(601))).isEqualTo(SituacaoSla.ATRASADO);
        assertThat(ticket(item("a", "x", "grill", 600)).estaAtrasado(AGORA.plusSeconds(601))).isTrue();
    }

    @Test
    void tempo_alvo_do_ticket_e_o_maior_dos_itens() {
        assertThat(ticket(item("a", "x", "grill", 300), item("b", "x", "grill", 900)).tempoAlvoSegundos()).isEqualTo(900);
    }

    @Test
    void cancelar_ultimo_item_ativo_cancela_o_ticket() {
        var a = item("a", "x", "grill", 300);
        var b = item("b", "x", "grill", 300);
        TicketProducao t = ticket(a, b);

        assertThat(t.cancelarItem(a.itemComandaId(), AGORA)).isTrue();
        assertThat(t.status()).isEqualTo(StatusTicket.RECEBIDO);
        assertThat(t.cancelarItem(a.itemComandaId(), AGORA)).isFalse();
        t.cancelarItem(b.itemComandaId(), AGORA);
        assertThat(t.status()).isEqualTo(StatusTicket.CANCELADO);
        assertThat(t.itensAtivosDaComanda()).isEmpty();
    }

    @Test
    void roteamento_respeita_a_precedencia_da_secao_8_5() {
        var regra = new RegraRoteamento("cozinha", List.of(
                new RegraRoteamento.Regra("hamb-vegano", null, "chapa-vegana", 10),
                new RegraRoteamento.Regra(null, "Bebidas", "bar", 1)));

        assertThat(RoteadorDeEstacao.estacaoDe(regra, item("hamb-vegano", "Lanches", "grill", 1))).isEqualTo("chapa-vegana");
        assertThat(RoteadorDeEstacao.estacaoDe(regra, item("suco", "Bebidas", "cozinha", 1))).isEqualTo("bar");
        assertThat(RoteadorDeEstacao.estacaoDe(regra, item("batata", "Porcoes", "Fritura", 1))).isEqualTo("fritura");
        assertThat(RoteadorDeEstacao.estacaoDe(regra, item("x", "Outros", null, 1))).isEqualTo("cozinha");
        assertThat(RoteadorDeEstacao.estacaoDe(RegraRoteamento.vazia(), item("x", "Outros", "", 1))).isEqualTo("geral");
    }

    @Test
    void itens_de_estacoes_diferentes_geram_grupos_diferentes() {
        var grupos = RoteadorDeEstacao.agruparPorEstacao(RegraRoteamento.vazia(), List.of(
                item("hamb", "Lanches", "grill", 1), item("batata", "Porcoes", "fritura", 1),
                item("picanha", "Pratos", "grill", 1)));
        assertThat(grupos).containsOnlyKeys("grill", "fritura");
        assertThat(grupos.get("grill")).hasSize(2);
    }
}
