package br.com.smartkds.catalogo.shared.dominio;

public record OpcaoModificador(String nome, long precoAdicionalCentavos, boolean disponivel) {
    public OpcaoModificador {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("nome da opcao e obrigatorio");
        }
        if (precoAdicionalCentavos < 0) {
            throw new RegraDeNegocioException("preco adicional nao pode ser negativo");
        }
    }
}
