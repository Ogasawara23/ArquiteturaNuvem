package br.com.smartkds.catalogo.shared.tenancy;

import br.com.smartkds.catalogo.shared.dominio.TenantId;

/**
 * Tenant da requisicao corrente. Populado pelo {@link FiltroDeTenant} a partir do
 * cabecalho X-Tenant-Id, que o API Gateway sempre sobrescreve com a claim do JWT.
 */
public final class ContextoDoTenant {

    private static final ThreadLocal<TenantId> ATUAL = new ThreadLocal<>();

    private ContextoDoTenant() { }

    public static TenantId atual() {
        TenantId tenant = ATUAL.get();
        if (tenant == null) {
            throw new TenantAusenteException();
        }
        return tenant;
    }

    static void definir(TenantId tenantId) {
        ATUAL.set(tenantId);
    }

    static void limpar() {
        ATUAL.remove();
    }
}
