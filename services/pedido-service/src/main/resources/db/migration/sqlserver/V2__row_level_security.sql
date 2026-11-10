-- Terceira camada do isolamento multi-tenant (Secao 8.2, ADR-12) no Azure SQL.
-- O DataSourceComTenant executa sp_set_session_context 'tenant_id' a cada conexao emprestada do pool.
-- Processos de sistema sem tenant (relay da outbox, migracoes) usam o valor vazio.
CREATE SCHEMA seguranca;
GO

CREATE FUNCTION seguranca.fn_mesmo_tenant(@tenant_id NVARCHAR(80))
    RETURNS TABLE
    WITH SCHEMABINDING
AS
    RETURN SELECT 1 AS permitido
     WHERE ISNULL(CAST(SESSION_CONTEXT(N'tenant_id') AS NVARCHAR(80)), N'') = N''
        OR @tenant_id = CAST(SESSION_CONTEXT(N'tenant_id') AS NVARCHAR(80));
GO

CREATE SECURITY POLICY seguranca.isolamento_por_tenant
    ADD FILTER PREDICATE seguranca.fn_mesmo_tenant(tenant_id) ON dbo.comanda,
    ADD BLOCK PREDICATE seguranca.fn_mesmo_tenant(tenant_id) ON dbo.comanda AFTER INSERT,
    ADD BLOCK PREDICATE seguranca.fn_mesmo_tenant(tenant_id) ON dbo.comanda AFTER UPDATE,
    ADD FILTER PREDICATE seguranca.fn_mesmo_tenant(tenant_id) ON dbo.log_auditoria,
    ADD BLOCK PREDICATE seguranca.fn_mesmo_tenant(tenant_id) ON dbo.log_auditoria AFTER INSERT
    WITH (STATE = ON);
GO
