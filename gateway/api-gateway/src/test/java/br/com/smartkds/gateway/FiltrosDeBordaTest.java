package br.com.smartkds.gateway;

import br.com.smartkds.gateway.borda.BaldeDeTokens;
import br.com.smartkds.gateway.borda.FiltroDeContexto;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FiltrosDeBordaTest {

    private static ServerWebExchange executar(FiltroDeContexto filtro, MockServerWebExchange troca) {
        AtomicReference<ServerWebExchange> repassada = new AtomicReference<>();
        GatewayFilterChain cadeia = t -> {
            repassada.set(t);
            return Mono.empty();
        };
        filtro.filter(troca, cadeia).block();
        return repassada.get();
    }

    @Test
    void cliente_nao_consegue_forjar_tenant_quando_ha_jwt() {
        var jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("usuario-1")
                .claim("tenant_id", "rest-trattoria-centro").issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60)).build();
        Principal principal = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_COZINHA")));
        var requisicao = MockServerHttpRequest.get("/bff/kds/estacoes/grill/tickets")
                .header(FiltroDeContexto.TENANT, "outro-restaurante").build();
        var troca = MockServerWebExchange.builder(requisicao).principal(principal).build();

        var resultado = executar(new FiltroDeContexto(true, "demo"), troca).getRequest().getHeaders();

        assertThat(resultado.getFirst(FiltroDeContexto.TENANT)).isEqualTo("rest-trattoria-centro");
        assertThat(resultado.getFirst(FiltroDeContexto.USUARIO)).isEqualTo("usuario-1");
        assertThat(resultado.getFirst(FiltroDeContexto.PAPEIS)).isEqualTo("COZINHA");
        assertThat(resultado.getFirst(FiltroDeContexto.CORRELACAO)).isNotBlank();
    }

    @Test
    void rota_publica_de_mesa_nunca_repassa_tenant_do_cliente() {
        var requisicao = MockServerHttpRequest.get("/bff/mesa/demo-mesa-1/cardapio")
                .header(FiltroDeContexto.TENANT, "tentativa").header(FiltroDeContexto.CORRELACAO, "abc").build();
        var resultado = executar(new FiltroDeContexto(false, "demo"),
                MockServerWebExchange.from(requisicao)).getRequest().getHeaders();

        assertThat(resultado.containsKey(FiltroDeContexto.TENANT)).isFalse();
        assertThat(resultado.getFirst(FiltroDeContexto.CORRELACAO)).isEqualTo("abc");
    }

    @Test
    void modo_demonstracao_usa_tenant_padrao() {
        var requisicao = MockServerHttpRequest.get("/bff/admin/aggregated-data").build();
        var resultado = executar(new FiltroDeContexto(false, "demo"),
                MockServerWebExchange.from(requisicao)).getRequest().getHeaders();
        assertThat(resultado.getFirst(FiltroDeContexto.TENANT)).isEqualTo("demo");
    }

    @Test
    void balde_permite_rajada_e_depois_limita_ate_reabastecer() {
        long t0 = 0;
        var balde = new BaldeDeTokens(1, 3, t0);
        assertThat(balde.consumir(t0)).isTrue();
        assertThat(balde.consumir(t0)).isTrue();
        assertThat(balde.consumir(t0)).isTrue();
        assertThat(balde.consumir(t0)).isFalse();
        assertThat(balde.consumir(t0 + 1_000_000_000L)).isTrue();
    }
}
