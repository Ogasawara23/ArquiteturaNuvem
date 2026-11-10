package br.com.smartkds.estabelecimento.shared.dominio;

public record TenantId(String valor) {
    public TenantId {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeNegocioException("tenantId e obrigatorio");
        }
    }
}
