package br.com.smartkds.estabelecimento.shared.web;

import br.com.smartkds.estabelecimento.shared.dominio.ConflitoException;
import br.com.smartkds.estabelecimento.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.estabelecimento.shared.dominio.RegraDeNegocioException;
import br.com.smartkds.estabelecimento.shared.tenancy.TenantAusenteException;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ManipuladorDeErros {

    @ExceptionHandler({RegraDeNegocioException.class, RequisicaoInvalidaException.class, TenantAusenteException.class})
    ProblemDetail requisicaoInvalida(RuntimeException e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisicao invalida", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisicao invalida", e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage()).collect(Collectors.joining("; ")));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Recurso nao encontrado", e.getMessage());
    }

    @ExceptionHandler({ConflitoException.class, DataIntegrityViolationException.class})
    ProblemDetail conflito(RuntimeException e) {
        return problema(HttpStatus.CONFLICT, "Conflito",
                e instanceof ConflitoException ? e.getMessage() : "registro duplicado ou referencia inexistente");
    }

    @ExceptionHandler(DataAccessException.class)
    ProblemDetail indisponivel(DataAccessException e) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Dependencia indisponivel", "banco temporariamente indisponivel");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setProperty("correlationId", MDC.get("correlation_id"));
        return problema;
    }
}
