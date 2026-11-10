package br.com.smartkds.catalogo.shared.web;

import br.com.smartkds.catalogo.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.catalogo.shared.dominio.RegraDeNegocioException;
import br.com.smartkds.catalogo.shared.dominio.VersaoInvalidaException;
import br.com.smartkds.catalogo.shared.tenancy.TenantAusenteException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/** Traduz excecoes de dominio para application/problem+json (RFC 9457). */
@RestControllerAdvice
public class ManipuladorDeErros {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Recurso nao encontrado", e.getMessage());
    }

    @ExceptionHandler(VersaoInvalidaException.class)
    ProblemDetail versaoInvalida(VersaoInvalidaException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Versao invalida", e.getMessage());
    }

    @ExceptionHandler({RegraDeNegocioException.class, TenantAusenteException.class})
    ProblemDetail requisicaoInvalida(RuntimeException e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisicao invalida", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException e) {
        String detalhe = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problema(HttpStatus.BAD_REQUEST, "Requisicao invalida", detalhe);
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    ProblemDetail dependenciaIndisponivel(org.springframework.dao.DataAccessException e) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Dependencia indisponivel",
                "banco de dados temporariamente indisponivel");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setProperty("correlationId", MDC.get("correlation_id"));
        return problema;
    }
}
