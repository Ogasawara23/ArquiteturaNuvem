package br.com.smartkds.gateway.borda;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Validacao do JWT emitido pelo Keycloak (assinatura via JWKS, emissor e expiracao) e
 * autorizacao grossa por experiencia. A autorizacao fina e reafirmada no BFF e no servico (ADR-04).
 * As rotas de mesa (/bff/mesa/**) sao publicas: a credencial e o qrToken, resolvido pelo bff-mesa.
 */
@Configuration
@EnableWebFluxSecurity
class ConfiguracaoDeSeguranca {

    @Bean
    SecurityWebFilterChain cadeiaDeSeguranca(ServerHttpSecurity http,
                                             @Value("${smartkds.seguranca.jwt-habilitado}") boolean jwtHabilitado) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable);
        if (!jwtHabilitado) {
            return http.authorizeExchange(regras -> regras.anyExchange().permitAll()).build();
        }
        return http
                .authorizeExchange(regras -> regras
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .pathMatchers("/bff/mesa/**").permitAll()
                        .pathMatchers("/bff/*/docs/**", "/bff/*/openapi.json", "/bff/admin/eventos",
                                "/bff/admin/asyncapi.yaml", "/docs/**").permitAll()
                        .pathMatchers("/bff/kds/**").hasAnyRole("COZINHA", "GERENTE", "ADMIN")
                        .pathMatchers("/bff/admin/**").hasAnyRole("GERENTE", "ATENDENTE", "ADMIN")
                        .anyExchange().denyAll())
                .oauth2ResourceServer(rs -> rs
                        .bearerTokenConverter(conversorDeToken())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDePapeis())))
                .build();
    }

    /**
     * EventSource (SSE) nao permite enviar cabecalho Authorization: para os streams o
     * navegador envia ?access_token=..., aceito apenas pelo resource server do gateway.
     */
    static org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter conversorDeToken() {
        var conversor = new org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter();
        conversor.setAllowUriQueryParameter(true);
        return conversor;
    }

    /** Converte realm_access.roles do Keycloak em ROLE_* do Spring Security. */
    static Converter<Jwt, Mono<AbstractAuthenticationToken>> conversorDePapeis() {
        return jwt -> Mono.just(new JwtAuthenticationToken(jwt, papeis(jwt), jwt.getSubject()));
    }

    @SuppressWarnings("unchecked")
    static Collection<GrantedAuthority> papeis(Jwt jwt) {
        Object acesso = jwt.getClaims().get("realm_access");
        if (!(acesso instanceof Map<?, ?> mapa) || !(mapa.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return ((Collection<Object>) roles).stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()))
                .toList();
    }
}
