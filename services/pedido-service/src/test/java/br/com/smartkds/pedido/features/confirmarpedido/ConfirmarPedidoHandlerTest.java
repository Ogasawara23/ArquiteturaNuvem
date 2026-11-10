package br.com.smartkds.pedido.features.confirmarpedido;

import br.com.smartkds.pedido.dominio.ComandaTest;
import br.com.smartkds.pedido.features.Fakes;
import br.com.smartkds.pedido.shared.dominio.SelecaoDeModificador;
import br.com.smartkds.pedido.shared.dominio.TenantId;
import br.com.smartkds.pedido.shared.eventos.PedidoConfirmado;
import br.com.smartkds.pedido.shared.web.RequisicaoInvalidaException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmarPedidoHandlerTest {

    private final Fakes.Comandas comandas = new Fakes.Comandas();
    private final Fakes.Publicador publicador = new Fakes.Publicador();
    private final ConfirmarPedidoHandler handler = new ConfirmarPedidoHandler(comandas, publicador, Fakes.relogio());

    @Test
    void grava_o_evento_na_outbox_exatamente_uma_vez_mesmo_com_reenvio() {
        var comanda = ComandaTest.comandaAberta();
        comanda.adicionarItem(ComandaTest.hamburguer(), 1,
                List.of(new SelecaoDeModificador("Ponto da carne", "Ao ponto")), "sem cebola", Fakes.relogio().instant());
        comandas.salvar(comanda);
        var comando = new ConfirmarPedidoCommand(comanda.tenantId(), comanda.id().valor(), "mesa12-1");

        var resposta = handler.executar(comando);
        var reenvio = handler.executar(comando);

        assertThat(resposta.itensConfirmados()).hasSize(1);
        assertThat(reenvio.repeticao()).isTrue();
        assertThat(publicador.publicados).hasSize(1);
        var evento = (PedidoConfirmado) publicador.publicados.get(0);
        assertThat(evento.routingKey()).isEqualTo("pedido.confirmado");
        assertThat(evento.mesaRotulo()).isEqualTo("Mesa 12");
        assertThat(evento.itens().get(0).estacaoSugerida()).isEqualTo("grill");
        assertThat(evento.itens().get(0).modificadores()).containsExactly("Ao ponto");
    }

    @Test
    void chave_de_idempotencia_e_obrigatoria() {
        assertThatThrownBy(() -> handler.executar(new ConfirmarPedidoCommand(new TenantId("t"),
                java.util.UUID.randomUUID(), " ")))
                .isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    void comanda_de_outro_tenant_nao_e_encontrada() {
        var comanda = ComandaTest.comandaAberta();
        comandas.salvar(comanda);
        assertThatThrownBy(() -> handler.executar(new ConfirmarPedidoCommand(new TenantId("outro-restaurante"),
                comanda.id().valor(), "k")))
                .hasMessageContaining("nao encontrado");
    }
}
