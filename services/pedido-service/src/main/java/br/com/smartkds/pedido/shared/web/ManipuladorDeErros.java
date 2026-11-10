package br.com.smartkds.pedido.shared.web;

import br.com.smartkds.pedido.shared.dominio.ComandaJaFechadaException;
import br.com.smartkds.pedido.shared.dominio.ItemIndisponivelException;
import br.com.smartkds.pedido.shared.dominio.ItemNaoCancelavelException;
import br.com.smartkds.pedido.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.pedido.shared.dominio.RegraDeNegocioException;
import br.com.smartkds.pedido.shared.dominio.TransicaoInvalidaException;
import br.com.smartkds.pedido.shared.tenancy.TenantAusenteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Traduz excecoes de dominio para application/problem+json (RFC 9457, Secao 8.6).
 * Nunca expoe pilha, nome de tabela ou identificador de outro tenant.
 */
@RestControllerAdvice
public class ManipuladorDeErros {

    private static final Logger LOG = LoggerFactory.getLogger(ManipuladorDeErros.class);

    @ExceptionHandler({RegraDeNegocioException.class, RequisicaoInvalidaException.class, TenantAusenteException.class})
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

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Recurso nao encontrado", e.getMessage());
    }

    @ExceptionHandler({TransicaoInvalidaException.class, ComandaJaFechadaException.class,
            ItemNaoCancelavelException.class, ItemIndisponivelException.class})
    ProblemDetail conflito(RuntimeException e) {
        return problema(HttpStatus.CONFLICT, "Operacao nao permitida neste estado", e.getMessage());
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ProblemDetail concorrencia(RuntimeException e) {
        return problema(HttpStatus.CONFLICT, "Conflito de concorrencia",
                "a comanda foi alterada por outra requisicao; tente novamente");
    }

    @ExceptionHandler({DependenciaIndisponivelException.class, DataAccessException.class})
    ProblemDetail indisponivel(RuntimeException e) {
        LOG.warn("dependencia indisponivel: {}", e.getMessage());
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Dependencia indisponivel",
                "servico temporariamente indisponivel");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setProperty("correlationId", MDC.get("correlation_id"));
        return problema;
    }
}
