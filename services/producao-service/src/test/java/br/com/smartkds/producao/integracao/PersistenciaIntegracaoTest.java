package br.com.smartkds.producao.integracao;

import br.com.smartkds.producao.dominio.TicketProducaoTest;
import br.com.smartkds.producao.shared.dominio.StatusTicket;
import br.com.smartkds.producao.shared.dominio.TenantId;
import br.com.smartkds.producao.shared.dominio.TicketProducao;
import br.com.smartkds.producao.shared.persistencia.IndicadoresRepository;
import br.com.smartkds.producao.shared.persistencia.TicketRepository;
import br.com.smartkds.producao.shared.tenancy.ContextoDoTenant;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/** PostgreSQL 16 real embutido: mapeamento do ticket, fila do KDS, compensacao e SQL de indicadores. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "smartkds.outbox.intervalo-ms=3600000"
})
class PersistenciaIntegracaoTest {

    private static final EmbeddedPostgres POSTGRES = iniciar();

    @Autowired
    TicketRepository tickets;
    @Autowired
    IndicadoresRepository indicadores;

    private static EmbeddedPostgres iniciar() {
        try {
            EmbeddedPostgres pg = EmbeddedPostgres.builder().start();
            try (Connection c = pg.getPostgresDatabase().getConnection(); Statement s = c.createStatement()) {
                s.execute("CREATE ROLE producao LOGIN PASSWORD 'producao' NOSUPERUSER");
                s.execute("CREATE DATABASE smartkds_producao OWNER producao");
            }
            return pg;
        } catch (IOException | SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("producao", "smartkds_producao"));
        r.add("spring.datasource.username", () -> "producao");
        r.add("spring.datasource.password", () -> "producao");
    }

    private static <T> T como(TenantId t, Supplier<T> acao) {
        return ContextoDoTenant.executarComo(t, "teste", null, acao);
    }

    @Test
    void fila_do_kds_compensacao_e_indicadores() {
        TenantId tenant = new TenantId("t-" + UUID.randomUUID());
        Instant criado = Instant.now().minusSeconds(900);
        var hamb = TicketProducaoTest.item("hamb", "Lanches", "grill", 600);
        var picanha = TicketProducaoTest.item("picanha", "Pratos", "grill", 600);
        TicketProducao ativo = TicketProducao.criar(tenant, UUID.randomUUID(), "Mesa 3", "grill", List.of(hamb), criado);
        TicketProducao expedido = TicketProducao.criar(tenant, UUID.randomUUID(), "Mesa 4", "grill", List.of(picanha), criado);
        expedido.avancarStatus(StatusTicket.EM_PREPARO, criado.plusSeconds(10));
        expedido.avancarStatus(StatusTicket.PRONTO, criado.plusSeconds(700));
        expedido.expedir(criado.plusSeconds(710));
        como(tenant, () -> { tickets.salvarTodos(List.of(ativo, expedido)); return null; });

        var fila = como(tenant, () -> tickets.listarAtivosPorEstacao(tenant, "grill"));
        assertThat(fila).extracting(TicketProducao::id).containsExactly(ativo.id());
        assertThat(fila.get(0).itens().get(0).modificadores()).containsExactly("Ao ponto");

        var afetados = como(tenant, () -> tickets.buscarPorItemDaComanda(tenant, hamb.itemComandaId()));
        afetados.get(0).cancelarItem(hamb.itemComandaId(), Instant.now());
        como(tenant, () -> tickets.salvar(afetados.get(0)));
        assertThat(como(tenant, () -> tickets.listarAtivosPorEstacao(tenant, "grill"))).isEmpty();

        var ind = como(tenant, () -> indicadores.calcular(tenant, criado.minusSeconds(60), "America/Sao_Paulo"));
        var grill = ind.estacoes().get(0);
        assertThat(grill.estacaoId()).isEqualTo("grill");
        assertThat(grill.ticketsCriados()).isEqualTo(2);
        assertThat(grill.ticketsExpedidos()).isEqualTo(1);
        assertThat(grill.ticketsAtrasados()).isEqualTo(1);
        assertThat(grill.tempoMedioPreparoSegundos()).isEqualTo(700.0);
        assertThat(ind.volumePorHora()).isNotEmpty();

        // RLS: outro tenant nao enxerga a fila, mesmo pedindo explicitamente pelo tenant original
        TenantId intruso = new TenantId("intruso");
        assertThat(como(intruso, () -> tickets.buscarPorItemDaComanda(tenant, picanha.itemComandaId()))).isEmpty();
    }
}
