package br.com.smartkds.pedido.integracao;

import br.com.smartkds.pedido.dominio.ComandaTest;
import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.dominio.MesaId;
import br.com.smartkds.pedido.shared.dominio.SelecaoDeModificador;
import br.com.smartkds.pedido.shared.dominio.TenantId;
import br.com.smartkds.pedido.shared.eventos.ItemCancelado;
import br.com.smartkds.pedido.shared.outbox.PublicadorDeEventos;
import br.com.smartkds.pedido.shared.persistencia.ComandaRepository;
import br.com.smartkds.pedido.shared.tenancy.ContextoDoTenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato de persistencia verificado contra bancos reais: migracoes Flyway por fornecedor,
 * mapeamento do agregado, Transactional Outbox, Row Level Security e concorrencia otimista.
 * Subclasses: PostgreSQL embutido (sem Docker) e Azure SQL / SQL Server (Testcontainers).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "smartkds.outbox.intervalo-ms=3600000"
})
abstract class PersistenciaContratoTest {

    private static final TenantId TENANT_A = new TenantId("rest-trattoria-centro");
    private static final TenantId TENANT_B = new TenantId("rest-sushi-bairro");
    private static final Instant AGORA = Instant.parse("2026-10-15T15:40:00Z");

    @Autowired
    ComandaRepository repositorio;
    @Autowired
    PublicadorDeEventos publicador;
    @Autowired
    TransactionTemplate transacao;
    @Autowired
    JdbcTemplate jdbc;

    /** O tipo do parametro UUID difere entre os drivers JDBC. */
    protected abstract Object idJdbc(UUID id);

    private static <T> T como(TenantId tenant, java.util.function.Supplier<T> acao) {
        return ContextoDoTenant.executarComo(tenant, "teste", null, acao);
    }

    private static Comanda novaComanda(TenantId tenant, String mesa) {
        return Comanda.abrir(tenant, new MesaId(mesa), "Mesa " + mesa, "v-1", AGORA);
    }

    @Test
    void salva_e_restaura_o_agregado_com_itens_e_modificadores() {
        Comanda comanda = novaComanda(TENANT_A, "m-" + UUID.randomUUID());
        comanda.adicionarItem(ComandaTest.hamburguer(), 2, List.of(
                new SelecaoDeModificador("Ponto da carne", "Ao ponto"),
                new SelecaoDeModificador("Adicionais", "Bacon extra")), "sem cebola", AGORA);
        como(TENANT_A, () -> repositorio.salvar(comanda));

        Comanda lida = como(TENANT_A, () -> repositorio.buscarPorId(TENANT_A, comanda.id())).orElseThrow();

        assertThat(lida.itens()).hasSize(1);
        assertThat(lida.itens().get(0).modificadoresComoTexto()).containsExactly("Ao ponto", "Bacon extra");
        assertThat(lida.subtotal()).isEqualTo(comanda.subtotal());
        assertThat(lida.versao()).isZero();
    }

    @Test
    void evento_vai_para_a_outbox_somente_se_a_transacao_confirmar() {
        Comanda comanda = novaComanda(TENANT_A, "m-" + UUID.randomUUID());
        var evento = ItemCancelado.novo(TENANT_A.valor(), comanda.id().valor(), UUID.randomUUID(), "teste", AGORA);
        var eventoRevertido = ItemCancelado.novo(TENANT_A.valor(), comanda.id().valor(), UUID.randomUUID(), "x", AGORA);

        como(TENANT_A, () -> transacao.execute(s -> {
            repositorio.salvar(comanda);
            publicador.publicar(evento);
            return null;
        }));
        assertThatThrownBy(() -> como(TENANT_A, () -> transacao.execute(s -> {
            publicador.publicar(eventoRevertido);
            throw new IllegalStateException("falha simulada apos gravar o evento");
        }))).isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("SELECT payload FROM outbox_evento WHERE evento_id = ?",
                String.class, idJdbc(evento.eventoId()))).contains("\"tipo\":\"ItemCancelado\"");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_evento WHERE evento_id = ?",
                Integer.class, idJdbc(eventoRevertido.eventoId()))).isZero();
    }

    @Test
    void publicar_fora_de_transacao_e_proibido() {
        var evento = ItemCancelado.novo(TENANT_A.valor(), UUID.randomUUID(), UUID.randomUUID(), "x", AGORA);
        assertThatThrownBy(() -> publicador.publicar(evento))
                .hasMessageContaining("No existing transaction");
    }

    @Test
    void row_level_security_esconde_linhas_de_outro_tenant_mesmo_sem_filtro() {
        String mesa = "m-" + UUID.randomUUID();
        como(TENANT_A, () -> repositorio.salvar(novaComanda(TENANT_A, mesa)));

        // A consulta pede explicitamente o tenant A, mas a conexao esta marcada com o tenant B:
        List<Comanda> vistasPeloTenantB = como(TENANT_B, () -> repositorio.listar(TENANT_A, null, new MesaId(mesa)));
        List<Comanda> vistasPeloTenantA = como(TENANT_A, () -> repositorio.listar(TENANT_A, null, new MesaId(mesa)));

        assertThat(vistasPeloTenantB).isEmpty();
        assertThat(vistasPeloTenantA).hasSize(1);
    }

    @Test
    void escrita_concorrente_na_mesma_comanda_e_detectada() {
        Comanda original = novaComanda(TENANT_A, "m-" + UUID.randomUUID());
        como(TENANT_A, () -> repositorio.salvar(original));
        Comanda copia1 = como(TENANT_A, () -> repositorio.buscarPorId(TENANT_A, original.id())).orElseThrow();
        Comanda copia2 = como(TENANT_A, () -> repositorio.buscarPorId(TENANT_A, original.id())).orElseThrow();

        copia1.adicionarItem(ComandaTest.refrigerante(), 1, List.of(), null, AGORA);
        como(TENANT_A, () -> repositorio.salvar(copia1));
        copia2.adicionarItem(ComandaTest.refrigerante(), 3, List.of(), null, AGORA);

        assertThatThrownBy(() -> como(TENANT_A, () -> repositorio.salvar(copia2)))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void no_maximo_uma_comanda_aberta_por_mesa() {
        String mesa = "m-" + UUID.randomUUID();
        como(TENANT_A, () -> repositorio.salvar(novaComanda(TENANT_A, mesa)));
        assertThatThrownBy(() -> como(TENANT_A, () -> repositorio.salvar(novaComanda(TENANT_A, mesa))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
