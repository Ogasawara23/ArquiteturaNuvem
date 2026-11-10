package br.com.smartkds.pedido.shared.tenancy;

public class TenantAusenteException extends RuntimeException {
    public TenantAusenteException() {
        super("cabecalho X-Tenant-Id ausente");
    }
}
