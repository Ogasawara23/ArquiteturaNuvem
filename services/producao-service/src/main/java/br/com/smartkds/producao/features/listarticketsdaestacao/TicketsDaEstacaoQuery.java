package br.com.smartkds.producao.features.listarticketsdaestacao;

import br.com.smartkds.producao.shared.dominio.TenantId;
import br.com.smartkds.producao.shared.dominio.TicketProducao;
import br.com.smartkds.producao.shared.persistencia.TicketRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Projecao da fila do KDS: urgencia = fracao do tempo-alvo ja consumida (maior primeiro). */
@Component
class TicketsDaEstacaoQuery {

    private final TicketRepository repositorio;
    private final Clock relogio;

    TicketsDaEstacaoQuery(TicketRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    public List<TicketDaEstacaoDto> executar(TenantId tenantId, String estacaoId, int percentualAtencao) {
        Instant agora = Instant.now(relogio);
        return repositorio.listarAtivosPorEstacao(tenantId, estacaoId).stream()
                .sorted(Comparator.comparingDouble((TicketProducao t) ->
                        (double) t.decorrido(agora).toSeconds() / t.tempoAlvoSegundos()).reversed())
                .map(t -> new TicketDaEstacaoDto(t.id(), t.comandaId(), t.mesaRotulo(), t.estacaoId(),
                        t.status().name(), t.tempoAlvoSegundos(), t.decorrido(agora).toSeconds(),
                        t.situacaoSla(agora, percentualAtencao).name(), t.criadoEm(),
                        t.itens().stream().map(i -> new TicketDaEstacaoDto.Item(i.itemComandaId(), i.descricao(),
                                i.quantidade(), i.modificadores(), i.observacao(), i.cancelado())).toList()))
                .toList();
    }
}
