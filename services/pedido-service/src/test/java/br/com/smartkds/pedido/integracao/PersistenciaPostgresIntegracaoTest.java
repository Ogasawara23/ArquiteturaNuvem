package br.com.smartkds.pedido.integracao;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

/** Contrato de persistencia contra PostgreSQL 16 real embutido (sem Docker). */
class PersistenciaPostgresIntegracaoTest extends PersistenciaContratoTest {

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registro) {
        BancosDeTeste.registrarPostgres(registro);
    }

    @Override
    protected Object idJdbc(UUID id) {
        return id;
    }
}
