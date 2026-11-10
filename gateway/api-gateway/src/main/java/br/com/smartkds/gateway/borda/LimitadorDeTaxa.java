package br.com.smartkds.gateway.borda;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitacao de taxa por tenant (rotas autenticadas) e politica mais restritiva por IP nas
 * rotas publicas de mesa (leitura abusiva de qrToken). Excesso resulta em 429 Problem Details.
 * Executa depois do FiltroDeContexto, para que X-Tenant-Id ja seja confiavel.
 */
@Component
public class LimitadorDeTaxa implements GlobalFilter, Ordered {

    private final Map<String, BaldeDeTokens> baldes = new ConcurrentHashMap<>();
    private final double taxaTenant;
    private final double rajadaTenant;
    private final double taxaMesa;
    private final double rajadaMesa;

    public LimitadorDeTaxa(@Value("${smartkds.limite.requisicoes-por-segundo-tenant}") double taxaTenant,
                           @Value("${smartkds.limite.rajada-tenant}") double rajadaTenant,
                           @Value("${smartkds.limite.requisicoes-por-segundo-mesa}") double taxaMesa,
                           @Value("${smartkds.limite.rajada-mesa}") double rajadaMesa) {
        this.taxaTenant = taxaTenant;
        this.rajadaTenant = rajadaTenant;
        this.taxaMesa = taxaMesa;
        this.rajadaMesa = rajadaMesa;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange troca, GatewayFilterChain cadeia) {
        String caminho = troca.getRequest().getPath().value();
        if (!caminho.startsWith("/bff/") || caminho.endsWith("/stream")) {
            return cadeia.filter(troca); // streams SSE sao conexoes longas, nao rajadas
        }
        boolean publica = caminho.startsWith("/bff/mesa/");
        String chave = publica
                ? "ip:" + Optional.ofNullable(troca.getRequest().getRemoteAddress())
                        .map(InetSocketAddress::getHostString).orElse("desconhecido")
                : "tenant:" + Optional.ofNullable(troca.getRequest().getHeaders().getFirst(FiltroDeContexto.TENANT))
                        .orElse("anonimo");
        BaldeDeTokens balde = baldes.computeIfAbsent(chave, k -> publica
                ? new BaldeDeTokens(taxaMesa, rajadaMesa, System.nanoTime())
                : new BaldeDeTokens(taxaTenant, rajadaTenant, System.nanoTime()));
        if (balde.consumir(System.nanoTime())) {
            return cadeia.filter(troca);
        }
        var resposta = troca.getResponse();
        resposta.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        resposta.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        resposta.getHeaders().set("Retry-After", "1");
        byte[] corpo = ("{\"title\":\"Too Many Requests\",\"status\":429,\"detail\":\"limite de requisicoes excedido\","
                + "\"correlationId\":\"" + troca.getRequest().getHeaders().getFirst(FiltroDeContexto.CORRELACAO) + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return resposta.writeWith(Mono.just(resposta.bufferFactory().wrap(corpo)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
