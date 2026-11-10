package br.com.smartkds.producao.features.avancarstatusticket;

import br.com.smartkds.producao.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.producao.shared.dominio.TicketProducao;
import br.com.smartkds.producao.shared.eventos.TicketStatusAlterado;
import br.com.smartkds.producao.shared.outbox.PublicadorDeEventos;
import br.com.smartkds.producao.shared.persistencia.TicketRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
class AvancarStatusTicketHandler {

    private final TicketRepository repositorio;
    private final PublicadorDeEventos publicador;
    private final Clock relogio;

    AvancarStatusTicketHandler(TicketRepository repositorio, PublicadorDeEventos publicador, Clock relogio) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transactional
    public AvancarStatusTicketResponse executar(AvancarStatusTicketCommand comando) {
        TicketProducao ticket = repositorio.buscarPorId(comando.tenantId(), comando.ticketId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ticket", comando.ticketId()));
        Instant agora = Instant.now(relogio);
        ticket.avancarStatus(comando.novoStatus(), agora);
        repositorio.salvar(ticket);
        publicador.publicar(TicketStatusAlterado.novo(ticket.tenantId().valor(), ticket.id(), ticket.comandaId(),
                ticket.estacaoId(), ticket.status().name(), ticket.itensAtivosDaComanda(), agora));
        return new AvancarStatusTicketResponse(ticket.id(), ticket.status().name(), agora);
    }
}
