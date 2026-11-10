-- Terceira camada do isolamento multi-tenant (Secao 8.2, ADR-12).
-- O DataSourceComTenant executa set_config('app.tenant_id', <tenant>, false) a cada conexao emprestada.
-- Processos de sistema sem tenant (relay da outbox, migracoes) usam o valor vazio.

ALTER TABLE comanda ENABLE ROW LEVEL SECURITY;
ALTER TABLE comanda FORCE ROW LEVEL SECURITY;
CREATE POLICY isolamento_por_tenant ON comanda
    USING (coalesce(current_setting('app.tenant_id', true), '') = '' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (coalesce(current_setting('app.tenant_id', true), '') = '' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE log_auditoria ENABLE ROW LEVEL SECURITY;
ALTER TABLE log_auditoria FORCE ROW LEVEL SECURITY;
CREATE POLICY isolamento_por_tenant ON log_auditoria
    USING (coalesce(current_setting('app.tenant_id', true), '') = '' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (coalesce(current_setting('app.tenant_id', true), '') = '' OR tenant_id = current_setting('app.tenant_id', true));
