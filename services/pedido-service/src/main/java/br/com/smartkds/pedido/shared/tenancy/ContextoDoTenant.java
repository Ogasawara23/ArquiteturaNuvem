package br.com.smartkds.pedido.shared.tenancy;

import br.com.smartkds.pedido.shared.dominio.TenantId;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Contexto da requisicao ou da mensagem corrente: tenant, usuario, correlacao e traceparent.
 * Populado pelo FiltroDeTenant (HTTP) ou por {@link #executarComo} (consumidores AMQP).
 */
public final class ContextoDoTenant {

    record Dados(TenantId tenant, String usuario, String correlacao, String traceparent) { }

    private static final ThreadLocal<Dados> ATUAL = new ThreadLocal<>();

    private ContextoDoTenant() { }

    public static TenantId atual() {
        Dados dados = ATUAL.get();
        if (dados == null || dados.tenant() == null) {
            throw new TenantAusenteException();
        }
        return dados.tenant();
    }

    public static Optional<TenantId> atualSeHouver() {
        return Optional.ofNullable(ATUAL.get()).map(Dados::tenant);
    }

    public static String usuario() {
        Dados dados = ATUAL.get();
        return dados == null ? null : dados.usuario();
    }

    public static String correlacao() {
        Dados dados = ATUAL.get();
        return dados == null ? null : dados.correlacao();
    }

    public static String traceparent() {
        Dados dados = ATUAL.get();
        return dados == null ? null : dados.traceparent();
    }

    /** Usado pelos consumidores: o tenant vem no corpo do evento. */
    public static <T> T executarComo(TenantId tenantId, String correlacao, String traceparent, Supplier<T> acao) {
        Dados anterior = ATUAL.get();
        ATUAL.set(new Dados(tenantId, "sistema:evento", correlacao, traceparent));
        try {
            return acao.get();
        } finally {
            if (anterior == null) {
                ATUAL.remove();
            } else {
                ATUAL.set(anterior);
            }
        }
    }

    static void definir(Dados dados) {
        ATUAL.set(dados);
    }

    static void limpar() {
        ATUAL.remove();
    }
}
