-- Row Level Security (Secao 8.2). Ver DataSourceComTenant.
ALTER TABLE ticket_producao ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket_producao FORCE ROW LEVEL SECURITY;
CREATE POLICY isolamento_por_tenant ON ticket_producao
    USING (coalesce(current_setting('app.tenant_id', true), '') = '' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (coalesce(current_setting('app.tenant_id', true), '') = '' OR tenant_id = current_setting('app.tenant_id', true));
