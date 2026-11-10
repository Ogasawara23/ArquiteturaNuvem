package br.com.smartkds.producao.shared.dominio;

import java.time.Instant;
import java.util.List;

/** RF18: tempo medio de preparo por estacao, tickets atrasados e volume por faixa de horario. */
public record IndicadoresOperacionais(Instant desde, List<PorEstacao> estacoes, List<PorHora> volumePorHora) {

    public record PorEstacao(String estacaoId,
                             long ticketsCriados,
                             long ticketsExpedidos,
                             long ticketsAtivos,
                             long ticketsAtrasados,
                             double tempoMedioPreparoSegundos) {

        public double taxaDeAtraso() {
            long concluidos = ticketsExpedidos;
            return concluidos == 0 ? 0 : (double) ticketsAtrasados / concluidos;
        }
    }

    public record PorHora(int hora, long tickets) {
    }
}
