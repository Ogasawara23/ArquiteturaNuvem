package br.com.smartkds.pedido.features.registrarprogressodaproducao;

import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.ItemComandaId;
import br.com.smartkds.pedido.shared.inbox.EventoProcessadoRepository;
import br.com.smartkds.pedido.shared.persistencia.ComandaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Reflete no item da comanda o progresso do ticket (Figura 22): EM_PREPARO leva os itens
 * a EM_PRODUCAO e PRONTO os leva a PRONTO. A partir de EM_PRODUCAO o cliente nao cancela mais.
 */
@Component
class RegistrarProgressoHandler {

    private final ComandaRepository repositorio;
    private final EventoProcessadoRepository eventosProcessados;
    private final Clock relogio;

    RegistrarProgressoHandler(ComandaRepository repositorio, EventoProcessadoRepository eventosProcessados,
                              Clock relogio) {
        this.repositorio = repositorio;
        this.eventosProcessados = eventosProcessados;
        this.relogio = relogio;
    }

    @Transactional
    public void executar(RegistrarProgressoCommand comando) {
        if (eventosProcessados.jaProcessado(comando.eventoId())) {
            return;
        }
        List<ItemComandaId> ids = comando.itemComandaIds().stream().map(ItemComandaId::new).toList();
        repositorio.buscarPorId(comando.tenantId(), new ComandaId(comando.comandaId())).ifPresent(comanda -> {
            int alterados = switch (comando.statusDoTicket()) {
                case "EM_PREPARO" -> comanda.registrarInicioDaProducao(ids);
                case "PRONTO" -> comanda.registrarItensProntos(ids);
                default -> 0;
            };
            if (alterados > 0) {
                repositorio.salvar(comanda);
            }
        });
        eventosProcessados.marcarProcessado(comando.eventoId(), "TicketStatusAlterado", Instant.now(relogio));
    }
}
