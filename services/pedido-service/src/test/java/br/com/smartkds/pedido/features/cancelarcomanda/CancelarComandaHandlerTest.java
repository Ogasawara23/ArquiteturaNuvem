package br.com.smartkds.pedido.features.cancelarcomanda;

import br.com.smartkds.pedido.dominio.ComandaTest;
import br.com.smartkds.pedido.features.Fakes;
import br.com.smartkds.pedido.shared.eventos.ItemCancelado;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CancelarComandaHandlerTest {

    @Test
    void publica_compensacao_para_itens_na_cozinha_e_audita() {
        var comandas = new Fakes.Comandas();
        var publicador = new Fakes.Publicador();
        var auditoria = new Fakes.Auditoria();
        var comanda = ComandaTest.comandaAberta();
        comanda.adicionarItem(ComandaTest.refrigerante(), 1, List.of(), null, Fakes.relogio().instant());
        comanda.confirmarItensPendentes("k", Fakes.relogio().instant());
        comanda.adicionarItem(ComandaTest.refrigerante(), 1, List.of(), null, Fakes.relogio().instant());
        comandas.salvar(comanda);

        new CancelarComandaHandler(comandas, publicador, auditoria, Fakes.relogio())
                .executar(new CancelarComandaCommand(comanda.tenantId(), comanda.id().valor(), "teste", "atendente", "c-1"));

        assertThat(publicador.publicados).singleElement().isInstanceOf(ItemCancelado.class);
        assertThat(auditoria.registros.get(0).acao()).isEqualTo("COMANDA_CANCELADA");
    }
}
