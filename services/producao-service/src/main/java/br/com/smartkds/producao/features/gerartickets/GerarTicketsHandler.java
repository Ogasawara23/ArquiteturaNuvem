package br.com.smartkds.producao.features.gerartickets;

import br.com.smartkds.producao.shared.dominio.RegraRoteamento;
import br.com.smartkds.producao.shared.dominio.RoteadorDeEstacao;
import br.com.smartkds.producao.shared.dominio.TicketProducao;
import br.com.smartkds.producao.shared.eventos.TicketCriado;
import br.com.smartkds.producao.shared.inbox.EventoProcessadoRepository;
import br.com.smartkds.producao.shared.integracao.EstabelecimentoGateway;
import br.com.smartkds.producao.shared.outbox.PublicadorDeEventos;
import br.com.smartkds.producao.shared.persistencia.TicketRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Idempotencia: consulta jaProcessado(eventoId) antes de criar qualquer ticket e marca o
 * evento na MESMA transacao em que salva os tickets (Figura 16).
 */
@Component
class GerarTicketsHandler {

    private final TicketRepository repositorio;
    private final EventoProcessadoRepository eventosProcessados;
    private final EstabelecimentoGateway estabelecimento;
    private final PublicadorDeEventos publicador;
    private final Clock relogio;

    GerarTicketsHandler(TicketRepository repositorio, EventoProcessadoRepository eventosProcessados,
                        EstabelecimentoGateway estabelecimento, PublicadorDeEventos publicador, Clock relogio) {
        this.repositorio = repositorio;
        this.eventosProcessados = eventosProcessados;
        this.estabelecimento = estabelecimento;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transactional
    public List<TicketProducao> executar(GerarTicketsCommand comando) {
        if (eventosProcessados.jaProcessado(comando.eventoId())) {
            return List.of();
        }
        Instant agora = Instant.now(relogio);
        RegraRoteamento regra = estabelecimento.regraDeRoteamento(comando.tenantId());

        List<TicketProducao> tickets = new ArrayList<>();
        RoteadorDeEstacao.agruparPorEstacao(regra, comando.itens()).forEach((estacao, itens) ->
                tickets.add(TicketProducao.criar(comando.tenantId(), comando.comandaId(), comando.mesaRotulo(),
                        estacao, itens, agora)));

        repositorio.salvarTodos(tickets);
        tickets.forEach(t -> publicador.publicar(TicketCriado.novo(t.tenantId().valor(), t.id(), t.comandaId(),
                t.mesaRotulo(), t.estacaoId(), t.status().name(), t.tempoAlvoSegundos(), t.criadoEm(),
                t.itens().stream().map(i -> new TicketCriado.Item(i.itemComandaId(), i.descricao(), i.quantidade(),
                        i.modificadores(), i.observacao())).toList())));
        eventosProcessados.marcarProcessado(comando.eventoId(), "PedidoConfirmado", agora);
        return tickets;
    }
}
