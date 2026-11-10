package br.com.smartkds.pedido.dominio;

import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.dominio.ComandaJaFechadaException;
import br.com.smartkds.pedido.shared.dominio.Dinheiro;
import br.com.smartkds.pedido.shared.dominio.ItemNaoCancelavelException;
import br.com.smartkds.pedido.shared.dominio.MesaId;
import br.com.smartkds.pedido.shared.dominio.RegraDeNegocioException;
import br.com.smartkds.pedido.shared.dominio.SelecaoDeModificador;
import br.com.smartkds.pedido.shared.dominio.SnapshotItemCardapio;
import br.com.smartkds.pedido.shared.dominio.StatusComanda;
import br.com.smartkds.pedido.shared.dominio.StatusItem;
import br.com.smartkds.pedido.shared.dominio.TenantId;
import br.com.smartkds.pedido.shared.dominio.TransicaoInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testes unitarios puros: sem Spring, sem banco, sem broker. */
public class ComandaTest {

    static final TenantId TENANT = new TenantId("rest-trattoria-centro");
    static final Instant AGORA = Instant.parse("2026-10-15T15:40:00Z");

    public static SnapshotItemCardapio hamburguer() {
        var ponto = new SnapshotItemCardapio.GrupoDeOpcoes("Ponto da carne", true, 1, 1, List.of(
                new SnapshotItemCardapio.Opcao("Ao ponto", Dinheiro.zero(), true),
                new SnapshotItemCardapio.Opcao("Bem passado", Dinheiro.zero(), true)));
        var adicionais = new SnapshotItemCardapio.GrupoDeOpcoes("Adicionais", false, 0, 3, List.of(
                new SnapshotItemCardapio.Opcao("Bacon extra", Dinheiro.deCentavos(600), true),
                new SnapshotItemCardapio.Opcao("Cheddar extra", Dinheiro.deCentavos(400), false)));
        return new SnapshotItemCardapio("item-hamburguer", "Hamburguer Artesanal 180g", "Lanches", Dinheiro.deCentavos(4200),
                "grill", 720, List.of(ponto, adicionais));
    }

    public static SnapshotItemCardapio refrigerante() {
        return new SnapshotItemCardapio("item-refri", "Refrigerante lata", "Bebidas", Dinheiro.deCentavos(700), "bar", 60,
                List.of());
    }

    public static Comanda comandaAberta() {
        return Comanda.abrir(TENANT, new MesaId("mesa-12"), "Mesa 12", "v-1", AGORA);
    }

    @Test
    void preco_e_congelado_e_modificadores_somam_ao_total() {
        Comanda comanda = comandaAberta();
        var item = comanda.adicionarItem(hamburguer(), 2,
                List.of(new SelecaoDeModificador("Ponto da carne", "Ao ponto"),
                        new SelecaoDeModificador("Adicionais", "Bacon extra")), "sem cebola", AGORA);

        assertThat(item.status()).isEqualTo(StatusItem.PENDENTE);
        assertThat(item.total()).isEqualTo(Dinheiro.deCentavos((4200 + 600) * 2));
        assertThat(comanda.subtotal()).isEqualTo(Dinheiro.deCentavos(9600));
    }

    @Test
    void grupo_obrigatorio_nao_selecionado_e_recusado() {
        assertThatThrownBy(() -> comandaAberta().adicionarItem(hamburguer(), 1, List.of(), null, AGORA))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Ponto da carne");
    }

    @Test
    void opcao_indisponivel_e_recusada() {
        assertThatThrownBy(() -> comandaAberta().adicionarItem(hamburguer(), 1, List.of(
                new SelecaoDeModificador("Ponto da carne", "Ao ponto"),
                new SelecaoDeModificador("Adicionais", "Cheddar extra")), null, AGORA))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void confirmar_transiciona_pendentes_e_e_idempotente_pela_chave() {
        Comanda comanda = comandaAberta();
        comanda.adicionarItem(refrigerante(), 1, List.of(), null, AGORA);

        var primeira = comanda.confirmarItensPendentes("chave-1", AGORA);
        var repeticao = comanda.confirmarItensPendentes("chave-1", AGORA);

        assertThat(primeira.repeticao()).isFalse();
        assertThat(primeira.itens()).allMatch(i -> i.status() == StatusItem.CONFIRMADO);
        assertThat(repeticao.repeticao()).isTrue();
        assertThat(repeticao.itens()).hasSize(1);
    }

    @Test
    void confirmar_sem_pendentes_e_transicao_invalida() {
        assertThatThrownBy(() -> comandaAberta().confirmarItensPendentes("x", AGORA))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void item_em_producao_nao_pode_ser_cancelado() {
        Comanda comanda = comandaAberta();
        var item = comanda.adicionarItem(refrigerante(), 1, List.of(), null, AGORA);
        comanda.confirmarItensPendentes("c", AGORA);
        comanda.registrarInicioDaProducao(List.of(item.id()));

        assertThat(item.status()).isEqualTo(StatusItem.EM_PRODUCAO);
        assertThatThrownBy(() -> comanda.cancelarItem(item.id(), "desisti", AGORA))
                .isInstanceOf(ItemNaoCancelavelException.class)
                .hasMessageContaining("producao");
    }

    @Test
    void cancelados_nao_entram_no_subtotal_e_fechamento_aplica_taxa() {
        Comanda comanda = comandaAberta();
        var refri = comanda.adicionarItem(refrigerante(), 2, List.of(), null, AGORA);
        var outro = comanda.adicionarItem(refrigerante(), 1, List.of(), null, AGORA);
        comanda.cancelarItem(outro.id(), null, AGORA);
        comanda.confirmarItensPendentes("c", AGORA);

        var resumo = comanda.fechar(BigDecimal.TEN, AGORA);

        assertThat(refri.status()).isEqualTo(StatusItem.CONFIRMADO);
        assertThat(resumo.subtotal()).isEqualTo(Dinheiro.deCentavos(1400));
        assertThat(resumo.taxaServico()).isEqualTo(Dinheiro.deCentavos(140));
        assertThat(resumo.total()).isEqualTo(Dinheiro.deCentavos(1540));
        assertThat(comanda.status()).isEqualTo(StatusComanda.FECHADA);
    }

    @Test
    void comanda_fechada_e_imutavel() {
        Comanda comanda = comandaAberta();
        comanda.fechar(BigDecimal.ZERO, AGORA);

        assertThatThrownBy(() -> comanda.adicionarItem(refrigerante(), 1, List.of(), null, AGORA))
                .isInstanceOf(ComandaJaFechadaException.class);
        assertThat(comanda.marcarItensComoEntregues(List.of(), AGORA)).isZero();
    }

    @Test
    void fechar_com_itens_pendentes_e_recusado() {
        Comanda comanda = comandaAberta();
        comanda.adicionarItem(refrigerante(), 1, List.of(), null, AGORA);
        assertThatThrownBy(() -> comanda.fechar(BigDecimal.TEN, AGORA)).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void entrega_e_tolerante_a_reentrega() {
        Comanda comanda = comandaAberta();
        var item = comanda.adicionarItem(refrigerante(), 1, List.of(), null, AGORA);
        comanda.confirmarItensPendentes("c", AGORA);

        assertThat(comanda.marcarItensComoEntregues(List.of(item.id()), AGORA)).isEqualTo(1);
        assertThat(comanda.marcarItensComoEntregues(List.of(item.id()), AGORA)).isZero();
        assertThat(item.status()).isEqualTo(StatusItem.ENTREGUE);
    }
}
