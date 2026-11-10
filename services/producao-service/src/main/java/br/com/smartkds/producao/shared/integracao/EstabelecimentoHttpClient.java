package br.com.smartkds.producao.shared.integracao;

import br.com.smartkds.producao.shared.dominio.RegraRoteamento;
import br.com.smartkds.producao.shared.dominio.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Cliente com cache de 5 minutos e degradacao controlada (Figura 16, passos 10 a 16). */
@Component
public class EstabelecimentoHttpClient implements EstabelecimentoGateway {

    private static final Logger LOG = LoggerFactory.getLogger(EstabelecimentoHttpClient.class);

    record RegraDto(String itemCardapioId, String categoria, String estacaoId, int prioridade) { }

    record RoteamentoDto(String estacaoPadrao, List<RegraDto> regras) { }

    private record Entrada(RegraRoteamento regra, Instant expiraEm) { }

    private final RestClient http;
    private final Clock relogio;
    private final Duration validade;
    private final Map<String, Entrada> cache = new ConcurrentHashMap<>();

    EstabelecimentoHttpClient(RestClient.Builder construtor, Clock relogio,
                              @Value("${smartkds.estabelecimento-url}") String urlBase,
                              @Value("${smartkds.roteamento.cache-minutos:5}") long minutos) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(1));
        fabrica.setReadTimeout(Duration.ofSeconds(2));
        this.http = construtor.baseUrl(urlBase).requestFactory(fabrica).build();
        this.relogio = relogio;
        this.validade = Duration.ofMinutes(minutos);
    }

    @Override
    public RegraRoteamento regraDeRoteamento(TenantId tenantId) {
        Instant agora = Instant.now(relogio);
        Entrada emCache = cache.get(tenantId.valor());
        if (emCache != null && agora.isBefore(emCache.expiraEm())) {
            return emCache.regra();
        }
        try {
            RoteamentoDto dto = http.get().uri("/interno/roteamento")
                    .header("X-Tenant-Id", tenantId.valor())
                    .retrieve().body(RoteamentoDto.class);
            RegraRoteamento regra = dto == null ? RegraRoteamento.vazia() : new RegraRoteamento(dto.estacaoPadrao(),
                    dto.regras() == null ? List.of() : dto.regras().stream()
                            .map(r -> new RegraRoteamento.Regra(r.itemCardapioId(), r.categoria(), r.estacaoId(),
                                    r.prioridade()))
                            .toList());
            cache.put(tenantId.valor(), new Entrada(regra, agora.plus(validade)));
            return regra;
        } catch (RestClientException e) {
            LOG.warn("estabelecimento-service indisponivel; usando {} para o tenant {}",
                    emCache != null ? "regra em cache expirada" : "estacaoSugerida do evento", tenantId.valor());
            return emCache != null ? emCache.regra() : RegraRoteamento.vazia();
        }
    }
}
