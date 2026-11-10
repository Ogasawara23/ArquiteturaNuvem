package br.com.smartkds.producao.shared.web;

import br.com.smartkds.producao.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.producao.shared.dominio.RegraDeNegocioException;
import br.com.smartkds.producao.shared.dominio.TransicaoInvalidaException;
import br.com.smartkds.producao.shared.tenancy.TenantAusenteException;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/** Problem Details (RFC 9457) para todas as fatias do producao-service. */
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

    @ExceptionHandler(TransicaoInvalidaException.class)
    ProblemDetail conflito(TransicaoInvalidaException e) {
        return problema(HttpStatus.CONFLICT, "Operacao nao permitida neste estado", e.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concorrencia(OptimisticLockingFailureException e) {
        return problema(HttpStatus.CONFLICT, "Conflito de concorrencia", "o ticket foi alterado por outra estacao");
    }

    @ExceptionHandler({DependenciaIndisponivelException.class, DataAccessException.class})
    ProblemDetail indisponivel(RuntimeException e) {
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Dependencia indisponivel", "servico temporariamente indisponivel");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setProperty("correlationId", MDC.get("correlation_id"));
        return problema;
    }
}
