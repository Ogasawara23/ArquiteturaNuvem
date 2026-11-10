package br.com.smartkds.estabelecimento.shared.tenancy;

public class TenantAusenteException extends RuntimeException {
    public TenantAusenteException() {
        super("cabecalho X-Tenant-Id ausente");
    }
}
