package br.com.smartkds.pedido.integracao;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Bancos reais compartilhados pelos testes de integracao (um por JVM de teste). */
public final class BancosDeTeste {

    private static EmbeddedPostgres postgres;
    private static MSSQLServerContainer<?> sqlServer;

    private BancosDeTeste() { }

    /** PostgreSQL 16 embutido, sem Docker. Usuario NAO superusuario, para o RLS valer. */
    public static synchronized void registrarPostgres(DynamicPropertyRegistry registro) {
        if (postgres == null) {
            try {
                postgres = EmbeddedPostgres.builder().start();
                try (Connection c = postgres.getPostgresDatabase().getConnection(); Statement s = c.createStatement()) {
                    s.execute("CREATE ROLE pedido LOGIN PASSWORD 'pedido' NOSUPERUSER");
                    s.execute("CREATE DATABASE smartkds_pedido OWNER pedido");
                }
            } catch (IOException | SQLException e) {
                throw new IllegalStateException("nao foi possivel iniciar o PostgreSQL embutido", e);
            }
        }
        registro.add("spring.datasource.url", () -> postgres.getJdbcUrl("pedido", "smartkds_pedido"));
        registro.add("spring.datasource.username", () -> "pedido");
        registro.add("spring.datasource.password", () -> "pedido");
    }

    /** Motor do Azure SQL Database (SQL Server 2022) via Testcontainers: requer Docker. */
    @SuppressWarnings("resource")
    public static synchronized void registrarAzureSql(DynamicPropertyRegistry registro) {
        if (sqlServer == null) {
            sqlServer = new MSSQLServerContainer<>(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
                    .acceptLicense()
                    .withInitScript("mssql/init.sql");
            sqlServer.start();
        }
        registro.add("spring.datasource.url", () -> sqlServer.getJdbcUrl() + ";databaseName=smartkds_pedido");
        registro.add("spring.datasource.username", sqlServer::getUsername);
        registro.add("spring.datasource.password", sqlServer::getPassword);
    }
}
