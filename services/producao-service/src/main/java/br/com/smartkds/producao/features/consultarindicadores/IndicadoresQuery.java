package br.com.smartkds.producao.features.consultarindicadores;

import br.com.smartkds.producao.shared.dominio.RegraDeNegocioException;
import br.com.smartkds.producao.shared.dominio.TenantId;
import br.com.smartkds.producao.shared.persistencia.IndicadoresRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

@Component
class IndicadoresQuery {

    private final IndicadoresRepository repositorio;
    private final Clock relogio;

    IndicadoresQuery(IndicadoresRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    public IndicadoresDto executar(TenantId tenantId, int horas, String fuso) {
        try {
            ZoneId.of(fuso);
        } catch (RuntimeException e) {
            throw new RegraDeNegocioException("fuso horario invalido: " + fuso);
        }
        Instant agora = Instant.now(relogio);
        var ind = repositorio.calcular(tenantId, agora.minus(Duration.ofHours(horas)), fuso);
        return new IndicadoresDto(ind.desde(), agora,
                ind.estacoes().stream().map(e -> new IndicadoresDto.Estacao(e.estacaoId(), e.ticketsCriados(),
                        e.ticketsExpedidos(), e.ticketsAtivos(), e.ticketsAtrasados(), e.taxaDeAtraso(),
                        Math.round(e.tempoMedioPreparoSegundos()))).toList(),
                ind.volumePorHora().stream().map(h -> new IndicadoresDto.Hora(h.hora(), h.tickets())).toList());
    }
}
