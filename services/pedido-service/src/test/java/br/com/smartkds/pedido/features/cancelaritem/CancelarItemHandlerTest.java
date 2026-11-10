package br.com.smartkds.pedido.features.cancelaritem;

import br.com.smartkds.pedido.dominio.ComandaTest;
import br.com.smartkds.pedido.features.Fakes;
import br.com.smartkds.pedido.shared.eventos.ItemCancelado;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CancelarItemHandlerTest {

    private final Fakes.Comandas comandas = new Fakes.Comandas();
    private final Fakes.Publicador publicador = new Fakes.Publicador();
    private final Fakes.Auditoria auditoria = new Fakes.Auditoria();
    private final CancelarItemHandler handler =
            new CancelarItemHandler(comandas, publicador, auditoria, Fakes.relogio());

    @Test
    void item_pendente_e_cancelado_sem_efeito_na_cozinha() {
        var comanda = ComandaTest.comandaAberta();
        var item = comanda.adicionarItem(ComandaTest.refrigerante(), 1, List.of(), null, Fakes.relogio().instant());
        comandas.salvar(comanda);

        handler.executar(new CancelarItemCommand(comanda.tenantId(), comanda.id().valor(), item.id().valor(),
                "engano", "mesa", "corr-1"));

        assertThat(publicador.publicados).isEmpty();
        assertThat(auditoria.registros).hasSize(1);
        assertThat(auditoria.registros.get(0).acao()).isEqualTo("ITEM_CANCELADO");
    }

    @Test
    void item_confirmado_publica_ItemCancelado_para_compensar_o_ticket() {
        var comanda = ComandaTest.comandaAberta();
        var item = comanda.adicionarItem(ComandaTest.refrigerante(), 1, List.of(), null, Fakes.relogio().instant());
        comanda.confirmarItensPendentes("c", Fakes.relogio().instant());
        comandas.salvar(comanda);

        handler.executar(new CancelarItemCommand(comanda.tenantId(), comanda.id().valor(), item.id().valor(),
                null, "mesa", "corr-2"));

        assertThat(publicador.publicados).singleElement().isInstanceOf(ItemCancelado.class);
        assertThat(((ItemCancelado) publicador.publicados.get(0)).itemComandaId()).isEqualTo(item.id().valor());
    }
}
