package br.com.smartkds.producao.shared.dominio;

public class RecursoNaoEncontradoException extends DominioException {
    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " nao encontrado: " + id);
    }
}
