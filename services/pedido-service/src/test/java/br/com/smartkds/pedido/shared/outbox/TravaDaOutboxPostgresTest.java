package br.com.smartkds.pedido.shared.outbox;

import br.com.smartkds.pedido.integracao.BancosDeTeste;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class TravaDaOutboxPostgresTest extends TravaDaOutboxContratoTest {

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registro) {
        BancosDeTeste.registrarPostgres(registro);
    }
}
