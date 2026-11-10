package br.com.smartkds.pedido.shared.integracao;

import br.com.smartkds.pedido.shared.dominio.Dinheiro;
import br.com.smartkds.pedido.shared.dominio.SnapshotItemCardapio;
import br.com.smartkds.pedido.shared.dominio.TenantId;
import br.com.smartkds.pedido.shared.tenancy.ContextoDoTenant;
import br.com.smartkds.pedido.shared.web.DependenciaIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class CatalogoHttpClient implements CatalogoGateway {

    record VersaoAtiva(String versaoId) { }

    record Opcao(String nome, long precoAdicionalCentavos, boolean disponivel) { }

    record Grupo(String nome, boolean obrigatorio, int minimoSelecoes, int maximoSelecoes, List<Opcao> opcoes) { }

    record ItemNaVersao(String itemCardapioId, String nome, String categoria, long precoCentavos, String estacaoSugerida,
                        int tempoAlvoPreparoSegundos, List<Grupo> gruposModificadores) { }

    private final RestClient http;

    CatalogoHttpClient(RestClient.Builder construtor, @Value("${smartkds.catalogo-url}") String urlBase) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(2));
        fabrica.setReadTimeout(Duration.ofSeconds(3));
        this.http = construtor.baseUrl(urlBase).requestFactory(fabrica).build();
    }

    @Override
    public Optional<String> versaoAtiva(TenantId tenantId) {
        try {
            VersaoAtiva ativa = http.get().uri("/interno/cardapios/ativo")
                    .headers(h -> cabecalhos(h, tenantId))
                    .retrieve().body(VersaoAtiva.class);
            return Optional.ofNullable(ativa).map(VersaoAtiva::versaoId);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new DependenciaIndisponivelException("catalogo-service", e);
        }
    }

    @Override
    public Optional<SnapshotItemCardapio> obterItemDaVersao(TenantId tenantId, String versaoId, String itemId) {
        try {
            ItemNaVersao item = http.get().uri("/interno/cardapios/{versao}/itens/{item}", versaoId, itemId)
                    .headers(h -> cabecalhos(h, tenantId))
                    .retrieve().body(ItemNaVersao.class);
            return Optional.ofNullable(item).map(CatalogoHttpClient::paraSnapshot);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new DependenciaIndisponivelException("catalogo-service", e);
        }
    }

    private static void cabecalhos(org.springframework.http.HttpHeaders h, TenantId tenantId) {
        h.set("X-Tenant-Id", tenantId.valor());
        String correlacao = ContextoDoTenant.correlacao();
        if (correlacao != null) {
            h.set("X-Correlation-Id", correlacao);
        }
    }

    private static SnapshotItemCardapio paraSnapshot(ItemNaVersao i) {
        List<SnapshotItemCardapio.GrupoDeOpcoes> grupos = i.gruposModificadores() == null ? List.of()
                : i.gruposModificadores().stream().map(g -> new SnapshotItemCardapio.GrupoDeOpcoes(
                        g.nome(), g.obrigatorio(), g.minimoSelecoes(), g.maximoSelecoes(),
                        g.opcoes() == null ? List.of() : g.opcoes().stream()
                                .map(o -> new SnapshotItemCardapio.Opcao(o.nome(),
                                        Dinheiro.deCentavos(o.precoAdicionalCentavos()), o.disponivel()))
                                .toList()))
                .toList();
        return new SnapshotItemCardapio(i.itemCardapioId(), i.nome(), i.categoria(), Dinheiro.deCentavos(i.precoCentavos()),
                i.estacaoSugerida(), i.tempoAlvoPreparoSegundos(), grupos);
    }
}
