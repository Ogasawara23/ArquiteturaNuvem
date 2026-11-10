package br.com.smartkds.pedido.features.registrarentregadopedido;

import br.com.smartkds.pedido.dominio.ComandaTest;
import br.com.smartkds.pedido.features.Fakes;
import br.com.smartkds.pedido.shared.dominio.StatusItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrarEntregaHandlerTest {

    private final Fakes.Comandas comandas = new Fakes.Comandas();
    private final Fakes.Inbox inbox = new Fakes.Inbox();
    private final RegistrarEntregaHandler handler = new RegistrarEntregaHandler(comandas, inbox, Fakes.relogio());

    @Test
    void mesmo_evento_entregue_duas_vezes_produz_um_unico_efeito() {
        var comanda = ComandaTest.comandaAberta();
        var item = comanda.adicionarItem(ComandaTest.refrigerante(), 1, List.of(), null, Fakes.relogio().instant());
        comanda.confirmarItensPendentes("c", Fakes.relogio().instant());
        comandas.salvar(comanda);
        int salvamentosAntes = comandas.salvamentos;
        var comando = new RegistrarEntregaCommand(UUID.randomUUID(), comanda.tenantId(), comanda.id().valor(),
                List.of(item.id().valor()));

        handler.executar(comando);
        handler.executar(comando);

        assertThat(item.status()).isEqualTo(StatusItem.ENTREGUE);
        assertThat(comandas.salvamentos - salvamentosAntes).isEqualTo(1);
        assertThat(inbox.processados).containsExactly(comando.eventoId());
    }
}
