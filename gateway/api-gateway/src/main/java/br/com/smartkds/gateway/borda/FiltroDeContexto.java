package br.com.smartkds.gateway.borda;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Primeira camada do isolamento multi-tenant (Secao 8.2): o gateway SEMPRE sobrescreve
 * X-Tenant-Id, X-User-Id e X-User-Roles, de modo que o cliente nao consegue forja-los.
 * Tambem gera ou propaga o X-Correlation-Id.
 */
@Component
public class FiltroDeContexto implements GlobalFilter, Ordered {

    public static final String TENANT = "X-Tenant-Id";
    public static final String USUARIO = "X-User-Id";
    public static final String PAPEIS = "X-User-Roles";
    public static final String CORRELACAO = "X-Correlation-Id";

    private final boolean jwtHabilitado;
    private final String tenantDemonstracao;

    public FiltroDeContexto(@Value("${smartkds.seguranca.jwt-habilitado}") boolean jwtHabilitado,
                            @Value("${smartkds.seguranca.tenant-demonstracao}") String tenantDemonstracao) {
        this.jwtHabilitado = jwtHabilitado;
        this.tenantDemonstracao = tenantDemonstracao;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange troca, GatewayFilterChain cadeia) {
        return troca.getPrincipal()
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(principal -> {
                    ServerHttpRequest requisicao = reescrever(troca.getRequest(), principal.orElse(null));
                    troca.getResponse().getHeaders().set(CORRELACAO, requisicao.getHeaders().getFirst(CORRELACAO));
                    return cadeia.filter(troca.mutate().request(requisicao).build());
                });
    }

    ServerHttpRequest reescrever(ServerHttpRequest original, Principal principal) {
        String caminho = original.getPath().value();
        String correlacao = Optional.ofNullable(original.getHeaders().getFirst(CORRELACAO))
                .filter(c -> !c.isBlank() && c.length() <= 80)
                .orElseGet(() -> UUID.randomUUID().toString());
        String tenantInformado = original.getHeaders().getFirst(TENANT);

        return original.mutate().headers(h -> {
            h.remove(TENANT);
            h.remove(USUARIO);
            h.remove(PAPEIS);
            h.set(CORRELACAO, correlacao);
            if (caminho.startsWith("/bff/mesa/")) {
                return; // o tenant da mesa e descoberto pelo bff-mesa a partir do qrToken
            }
            if (principal instanceof JwtAuthenticationToken jwt) {
                String tenant = jwt.getToken().getClaimAsString("tenant_id");
                if (tenant != null) {
                    h.set(TENANT, tenant);
                }
                h.set(USUARIO, jwt.getToken().getSubject());
                h.set(PAPEIS, jwt.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .map(a -> a.replaceFirst("^ROLE_", "")).collect(Collectors.joining(",")));
            } else if (!jwtHabilitado) {
                // modo demonstracao: sem Keycloak, o tenant vem do cabecalho ou do padrao configurado
                h.set(TENANT, tenantInformado == null || tenantInformado.isBlank() ? tenantDemonstracao : tenantInformado);
                h.set(USUARIO, "demonstracao");
                h.set(PAPEIS, "GERENTE,COZINHA,ATENDENTE");
            }
        }).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
