package br.com.smartkds.pedido.shared.dominio;

public record MesaId(String valor) {
    public MesaId {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeNegocioException("mesaId e obrigatorio");
        }
    }
}
