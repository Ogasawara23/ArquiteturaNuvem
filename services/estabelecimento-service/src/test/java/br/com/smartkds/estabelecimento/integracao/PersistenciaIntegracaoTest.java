package br.com.smartkds.estabelecimento.integracao;

import br.com.smartkds.estabelecimento.shared.dominio.Mesa;
import br.com.smartkds.estabelecimento.shared.dominio.RegraDeRoteamento;
import br.com.smartkds.estabelecimento.shared.dominio.TenantId;
import br.com.smartkds.estabelecimento.shared.persistencia.EstabelecimentoRepository;
import br.com.smartkds.estabelecimento.shared.persistencia.EstacaoRepository;
import br.com.smartkds.estabelecimento.shared.persistencia.MesaRepository;
import br.com.smartkds.estabelecimento.shared.persistencia.RegraRoteamentoRepository;
import br.com.smartkds.estabelecimento.shared.persistencia.ResolvedorDeQrToken;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** PostgreSQL real: migracoes, dados de demonstracao, resolucao do qrToken e substituicao de regras. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PersistenciaIntegracaoTest {

    private static final EmbeddedPostgres POSTGRES = iniciar();
    private static final TenantId DEMO = new TenantId("demo");

    @Autowired
    ResolvedorDeQrToken resolvedor;
    @Autowired
    MesaRepository mesas;
    @Autowired
    EstacaoRepository estacoes;
    @Autowired
    RegraRoteamentoRepository regras;
    @Autowired
    EstabelecimentoRepository estabelecimentos;

    private static EmbeddedPostgres iniciar() {
        try {
            EmbeddedPostgres pg = EmbeddedPostgres.builder().start();
            try (Connection c = pg.getPostgresDatabase().getConnection(); Statement s = c.createStatement()) {
                s.execute("CREATE ROLE estabelecimento LOGIN PASSWORD 'estabelecimento' NOSUPERUSER");
                s.execute("CREATE DATABASE smartkds_estabelecimento OWNER estabelecimento");
            }
            return pg;
        } catch (IOException | SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("estabelecimento", "smartkds_estabelecimento"));
        r.add("spring.datasource.username", () -> "estabelecimento");
        r.add("spring.datasource.password", () -> "estabelecimento");
    }

    @Test
    void dados_de_demonstracao_e_resolucao_do_qr_token() {
        var mesa = resolvedor.resolver("demo-mesa-1").orElseThrow();
        assertThat(mesa.tenantId()).isEqualTo(DEMO);
        assertThat(mesa.rotulo()).isEqualTo("Mesa 1");
        assertThat(resolvedor.resolver("token-inexistente")).isEmpty();
        assertThat(estacoes.listarEstacoes(DEMO)).extracting(e -> e.codigo())
                .containsExactly("grill", "fritura", "bar", "sobremesa", "cozinha");
        assertThat(estabelecimentos.buscar(DEMO).orElseThrow().estacaoPadrao()).isEqualTo("cozinha");
    }

    @Test
    void nova_mesa_e_rotacao_de_token() {
        Mesa mesa = Mesa.cadastrar(DEMO, "Varanda " + UUID.randomUUID().toString().substring(0, 4));
        mesas.salvar(mesa);
        String anterior = mesa.qrToken();
        mesa.rotacionarQrToken();
        mesas.salvar(mesa);

        assertThat(resolvedor.resolver(anterior)).isEmpty();
        assertThat(resolvedor.resolver(mesa.qrToken()).orElseThrow().id()).isEqualTo(mesa.id());
    }

    @Test
    void substituir_regras_e_idempotente() {
        var nova = List.of(new RegraDeRoteamento(UUID.randomUUID(), DEMO, "item-picanha", null, "grill", 5));
        regras.substituirTodas(DEMO, nova);
        regras.substituirTodas(DEMO, nova.stream()
                .map(r -> new RegraDeRoteamento(UUID.randomUUID(), DEMO, r.itemCardapioId(), null, "grill", 5)).toList());
        assertThat(regras.listarRegras(DEMO)).singleElement()
                .satisfies(r -> assertThat(r.itemCardapioId()).isEqualTo("item-picanha"));
    }
}
