package br.com.smartkds.gateway.borda;

/**
 * Token bucket: reabastece {@code taxaPorSegundo} fichas por segundo ate {@code capacidade}.
 * Equivalente local do RedisRateLimiter (replenishRate / burstCapacity); com varias replicas,
 * o limite efetivo e multiplicado pelo numero de replicas (registrado nos riscos).
 */
public final class BaldeDeTokens {

    private final double taxaPorSegundo;
    private final double capacidade;
    private double fichas;
    private long ultimoReabastecimentoNanos;

    public BaldeDeTokens(double taxaPorSegundo, double capacidade, long agoraNanos) {
        this.taxaPorSegundo = taxaPorSegundo;
        this.capacidade = capacidade;
        this.fichas = capacidade;
        this.ultimoReabastecimentoNanos = agoraNanos;
    }

    public synchronized boolean consumir(long agoraNanos) {
        double decorrido = Math.max(0, agoraNanos - ultimoReabastecimentoNanos) / 1_000_000_000.0;
        fichas = Math.min(capacidade, fichas + decorrido * taxaPorSegundo);
        ultimoReabastecimentoNanos = agoraNanos;
        if (fichas >= 1) {
            fichas -= 1;
            return true;
        }
        return false;
    }
}
