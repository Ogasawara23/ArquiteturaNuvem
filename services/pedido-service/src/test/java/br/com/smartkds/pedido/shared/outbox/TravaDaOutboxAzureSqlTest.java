package br.com.smartkds.pedido.shared.outbox;

import br.com.smartkds.pedido.integracao.BancosDeTeste;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class TravaDaOutboxAzureSqlTest extends TravaDaOutboxContratoTest {

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registro) {
        BancosDeTeste.registrarAzureSql(registro);
    }
}
