package br.com.smartkds.pedido.shared.dominio;

public record TenantId(String valor) {
    public TenantId {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeNegocioException("tenantId e obrigatorio");
        }
    }
}
