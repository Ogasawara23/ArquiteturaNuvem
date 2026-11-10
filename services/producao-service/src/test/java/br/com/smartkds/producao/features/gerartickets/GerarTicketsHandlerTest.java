package br.com.smartkds.producao.features.gerartickets;

import br.com.smartkds.producao.dominio.TicketProducaoTest;
import br.com.smartkds.producao.shared.dominio.RegraRoteamento;
import br.com.smartkds.producao.shared.dominio.TenantId;
import br.com.smartkds.producao.shared.dominio.TicketProducao;
import br.com.smartkds.producao.shared.eventos.EventoDeDominio;
import br.com.smartkds.producao.shared.eventos.TicketCriado;
import br.com.smartkds.producao.shared.inbox.EventoProcessadoRepository;
import br.com.smartkds.producao.shared.persistencia.TicketRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GerarTicketsHandlerTest {

    private final List<TicketProducao> salvos = new ArrayList<>();
    private final Set<UUID> processados = new HashSet<>();
    private final List<EventoDeDominio> publicados = new ArrayList<>();

    private final TicketRepository repositorio = new TicketRepository() {
        public void salvarTodos(List<TicketProducao> tickets) { salvos.addAll(tickets); }
        public TicketProducao salvar(TicketProducao t) { salvos.add(t); return t; }
        public Optional<TicketProducao> buscarPorId(TenantId tenantId, UUID id) { return Optional.empty(); }
        public List<TicketProducao> listarAtivosPorEstacao(TenantId tenantId, String e) { return salvos; }
        public List<TicketProducao> buscarPorItemDaComanda(TenantId tenantId, UUID i) { return List.of(); }
    };
    private final EventoProcessadoRepository inbox = new EventoProcessadoRepository() {
        public boolean jaProcessado(UUID eventoId) { return processados.contains(eventoId); }
        public void marcarProcessado(UUID eventoId, String origem, Instant agora) { processados.add(eventoId); }
    };

    private GerarTicketsHandler handler(RegraRoteamento regra) {
        return new GerarTicketsHandler(repositorio, inbox, tenant -> regra, publicados::add,
                Clock.fixed(Instant.parse("2026-10-15T15:40:00Z"), ZoneOffset.UTC));
    }

    @Test
    void entrega_duplicada_do_mesmo_evento_gera_um_unico_conjunto_de_tickets() {
        var comando = new GerarTicketsCommand(UUID.randomUUID(), new TenantId("t1"), UUID.randomUUID(), "Mesa 12",
                List.of(TicketProducaoTest.item("hamb", "Lanches", "grill", 720),
                        TicketProducaoTest.item("batata", "Porcoes", "fritura", 480),
                        TicketProducaoTest.item("refri", "Bebidas", "bar", 60)));
        var h = handler(RegraRoteamento.vazia());

        var primeira = h.executar(comando);
        var segunda = h.executar(comando);

        assertThat(primeira).hasSize(3);
        assertThat(segunda).isEmpty();
        assertThat(salvos).hasSize(3);
        assertThat(publicados).hasSize(3).allMatch(e -> e instanceof TicketCriado);
        assertThat(salvos).extracting(TicketProducao::estacaoId).containsExactlyInAnyOrder("grill", "fritura", "bar");
    }

    @Test
    void sem_estabelecimento_usa_a_estacao_sugerida_congelada_no_evento() {
        var comando = new GerarTicketsCommand(UUID.randomUUID(), new TenantId("t1"), UUID.randomUUID(), "Mesa 1",
                List.of(TicketProducaoTest.item("hamb", "Lanches", "grill", 720)));
        handler(RegraRoteamento.vazia()).executar(comando);
        assertThat(salvos.get(0).estacaoId()).isEqualTo("grill");
    }
}
