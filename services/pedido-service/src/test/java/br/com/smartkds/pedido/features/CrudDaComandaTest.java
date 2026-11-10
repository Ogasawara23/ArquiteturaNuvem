package br.com.smartkds.pedido.features;

import br.com.smartkds.pedido.dominio.ComandaTest;
import br.com.smartkds.pedido.shared.dominio.Dinheiro;
import br.com.smartkds.pedido.shared.dominio.StatusComanda;
import br.com.smartkds.pedido.shared.dominio.StatusItem;
import br.com.smartkds.pedido.shared.dominio.TransicaoInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Update e Delete do CRUD do Microsservico 2 (regras no agregado). */
class CrudDaComandaTest {

    private final Instant agora = Instant.parse("2026-10-15T15:40:00Z");

    @Test
    void altera_quantidade_e_observacao_apenas_de_item_no_carrinho() {
        var comanda = ComandaTest.comandaAberta();
        var refri = comanda.adicionarItem(ComandaTest.refrigerante(), 1, List.of(), null, agora);

        comanda.alterarItem(refri.id(), 3, "bem gelado");
        assertThat(refri.quantidade()).isEqualTo(3);
        assertThat(comanda.subtotal()).isEqualTo(Dinheiro.deCentavos(2100));

        comanda.confirmarItensPendentes("k", agora);
        assertThatThrownBy(() -> comanda.alterarItem(refri.id(), 1, null)).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void cancelar_comanda_compensa_apenas_itens_que_ja_estavam_na_cozinha() {
        var comanda = ComandaTest.comandaAberta();
        var enviado = comanda.adicionarItem(ComandaTest.refrigerante(), 1, List.of(), null, agora);
        comanda.confirmarItensPendentes("k", agora);
        comanda.adicionarItem(ComandaTest.refrigerante(), 2, List.of(), null, agora);

        var naCozinha = comanda.cancelar("cliente desistiu", agora);

        assertThat(naCozinha).extracting(i -> i.id()).containsExactly(enviado.id());
        assertThat(comanda.status()).isEqualTo(StatusComanda.CANCELADA);
        assertThat(comanda.itens()).allMatch(i -> i.status() == StatusItem.CANCELADO);
    }

    @Test
    void comanda_com_item_em_producao_nao_pode_ser_cancelada() {
        var comanda = ComandaTest.comandaAberta();
        var item = comanda.adicionarItem(ComandaTest.refrigerante(), 1, List.of(), null, agora);
        comanda.confirmarItensPendentes("k", agora);
        comanda.registrarInicioDaProducao(List.of(item.id()));
        assertThatThrownBy(() -> comanda.cancelar(null, agora)).isInstanceOf(TransicaoInvalidaException.class);
    }

}
