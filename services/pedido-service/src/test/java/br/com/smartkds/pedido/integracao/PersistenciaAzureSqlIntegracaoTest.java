package br.com.smartkds.pedido.integracao;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

/**
 * Contrato de persistencia contra o motor do Azure SQL Database (SQL Server 2022), incluindo o
 * Row Level Security por SESSION_CONTEXT. Executa onde houver Docker (GitHub Actions);
 * sem Docker e ignorado automaticamente.
 */
@Testcontainers(disabledWithoutDocker = true)
class PersistenciaAzureSqlIntegracaoTest extends PersistenciaContratoTest {

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registro) {
        BancosDeTeste.registrarAzureSql(registro);
    }

    @Override
    protected Object idJdbc(UUID id) {
        return id.toString();
    }
}
