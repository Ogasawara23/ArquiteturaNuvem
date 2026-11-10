package br.com.smartkds.pedido.shared.web;

/** Falha de dependencia interna (banco, broker ou outro servico) convertida em 503. */
public class DependenciaIndisponivelException extends RuntimeException {
    public DependenciaIndisponivelException(String dependencia, Throwable causa) {
        super(dependencia + " indisponivel", causa);
    }
}
