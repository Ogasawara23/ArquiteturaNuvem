package br.com.smartkds.producao.features.cancelarticketsdoitem;

import br.com.smartkds.producao.shared.dominio.TicketProducao;
import br.com.smartkds.producao.shared.eventos.TicketStatusAlterado;
import br.com.smartkds.producao.shared.inbox.EventoProcessadoRepository;
import br.com.smartkds.producao.shared.outbox.PublicadorDeEventos;
import br.com.smartkds.producao.shared.persistencia.TicketRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** Compensacao da saga coreografada (Figura 19, passos 17 a 22). */
@Component
class CancelarItemDoTicketHandler {

    private final TicketRepository repositorio;
    private final EventoProcessadoRepository eventosProcessados;
    private final PublicadorDeEventos publicador;
    private final Clock relogio;

    CancelarItemDoTicketHandler(TicketRepository repositorio, EventoProcessadoRepository eventosProcessados,
                                PublicadorDeEventos publicador, Clock relogio) {
        this.repositorio = repositorio;
        this.eventosProcessados = eventosProcessados;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transactional
    public void executar(CancelarItemDoTicketCommand comando) {
        if (eventosProcessados.jaProcessado(comando.eventoId())) {
            return;
        }
        Instant agora = Instant.now(relogio);
        for (TicketProducao ticket : repositorio.buscarPorItemDaComanda(comando.tenantId(), comando.itemComandaId())) {
            if (ticket.cancelarItem(comando.itemComandaId(), agora)) {
                repositorio.salvar(ticket);
                // notifica o stream do KDS: o cartao e atualizado (ou removido, se o ticket foi cancelado)
                publicador.publicar(TicketStatusAlterado.novo(ticket.tenantId().valor(), ticket.id(),
                        ticket.comandaId(), ticket.estacaoId(), ticket.status().name(),
                        ticket.itensAtivosDaComanda(), agora));
            }
        }
        eventosProcessados.marcarProcessado(comando.eventoId(), "ItemCancelado", agora);
    }
}
