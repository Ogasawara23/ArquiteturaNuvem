package br.com.smartkds.producao.features.expedirticket;

import br.com.smartkds.producao.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.producao.shared.dominio.TicketProducao;
import br.com.smartkds.producao.shared.eventos.TicketExpedido;
import br.com.smartkds.producao.shared.outbox.PublicadorDeEventos;
import br.com.smartkds.producao.shared.persistencia.TicketRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** Figura 18, passos 19 a 22: ticket EXPEDIDO e TicketExpedido gravados na mesma transacao. */
@Component
class ExpedirTicketHandler {

    private final TicketRepository repositorio;
    private final PublicadorDeEventos publicador;
    private final Clock relogio;

    ExpedirTicketHandler(TicketRepository repositorio, PublicadorDeEventos publicador, Clock relogio) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transactional
    public void executar(ExpedirTicketCommand comando) {
        TicketProducao ticket = repositorio.buscarPorId(comando.tenantId(), comando.ticketId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ticket", comando.ticketId()));
        Instant agora = Instant.now(relogio);
        ticket.expedir(agora);
        repositorio.salvar(ticket);
        publicador.publicar(TicketExpedido.novo(ticket.tenantId().valor(), ticket.id(), ticket.comandaId(),
                ticket.estacaoId(), ticket.itensAtivosDaComanda(), agora));
    }
}
