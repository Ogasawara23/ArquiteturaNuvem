package br.com.smartkds.pedido.features.registrarentregadopedido;

import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.ItemComandaId;
import br.com.smartkds.pedido.shared.inbox.EventoProcessadoRepository;
import br.com.smartkds.pedido.shared.persistencia.ComandaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Consome TicketExpedido de forma idempotente e marca os itens como ENTREGUE (Figura 18).
 * O efeito e a marcacao na inbox acontecem na mesma transacao.
 */
@Component
class RegistrarEntregaHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrarEntregaHandler.class);

    private final ComandaRepository repositorio;
    private final EventoProcessadoRepository eventosProcessados;
    private final Clock relogio;

    RegistrarEntregaHandler(ComandaRepository repositorio, EventoProcessadoRepository eventosProcessados,
                            Clock relogio) {
        this.repositorio = repositorio;
        this.eventosProcessados = eventosProcessados;
        this.relogio = relogio;
    }

    @Transactional
    public void executar(RegistrarEntregaCommand comando) {
        if (eventosProcessados.jaProcessado(comando.eventoId())) {
            LOG.info("evento {} ja processado; ignorando reentrega", comando.eventoId());
            return;
        }
        Instant agora = Instant.now(relogio);
        repositorio.buscarPorId(comando.tenantId(), new ComandaId(comando.comandaId())).ifPresentOrElse(comanda -> {
            int alterados = comanda.marcarItensComoEntregues(
                    comando.itemComandaIds().stream().map(ItemComandaId::new).toList(), agora);
            if (alterados > 0) {
                repositorio.salvar(comanda);
            }
        }, () -> LOG.warn("comanda {} inexistente para TicketExpedido {}", comando.comandaId(), comando.eventoId()));
        eventosProcessados.marcarProcessado(comando.eventoId(), "TicketExpedido", agora);
    }
}
