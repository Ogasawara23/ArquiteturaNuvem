package br.com.smartkds.pedido.shared.tenancy;

import br.com.smartkds.pedido.shared.dominio.TenantId;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Terceira camada do isolamento (Secao 8.2): a cada conexao emprestada do pool,
 * informa o tenant corrente ao banco, que aplica a politica de Row Level Security:
 * SESSION_CONTEXT no Azure SQL (sp_set_session_context) ou set_config no PostgreSQL.
 * Mesmo que uma consulta esqueca o filtro, o banco nao devolve linhas de outro tenant.
 */
public class DataSourceComTenant extends DelegatingDataSource {

    private volatile Boolean sqlServer;

    public DataSourceComTenant(DataSource alvo) {
        super(alvo);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return informarTenant(super.getConnection());
    }

    @Override
    public Connection getConnection(String usuario, String senha) throws SQLException {
        return informarTenant(super.getConnection(usuario, senha));
    }

    private Connection informarTenant(Connection conexao) throws SQLException {
        String tenant = ContextoDoTenant.atualSeHouver().map(TenantId::valor).orElse("");
        if (sqlServer == null) {
            sqlServer = conexao.getMetaData().getDatabaseProductName().toLowerCase().contains("sql server");
        }
        String sql = sqlServer
                ? "EXEC sp_set_session_context @key = N'tenant_id', @value = ?, @read_only = 0"
                : "select set_config('app.tenant_id', ?, false)";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, tenant);
            comando.execute();
        } catch (SQLException e) {
            conexao.close();
            throw e;
        }
        return conexao;
    }

    /** Envolve o DataSource do Spring Boot sem alterar a configuracao do Hikari. */
    @Component
    static class Envolvedor implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(Object bean, String nome) {
            if (bean instanceof DataSource ds && !(bean instanceof DataSourceComTenant) && "dataSource".equals(nome)) {
                return new DataSourceComTenant(ds);
            }
            return bean;
        }
    }
}
