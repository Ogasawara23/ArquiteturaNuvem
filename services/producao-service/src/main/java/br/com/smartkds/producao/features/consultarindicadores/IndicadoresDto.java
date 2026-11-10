package br.com.smartkds.producao.features.consultarindicadores;

import java.time.Instant;
import java.util.List;

record IndicadoresDto(Instant desde, Instant geradoEm, List<Estacao> estacoes, List<Hora> volumePorHora) {

    record Estacao(String estacaoId, long ticketsCriados, long ticketsExpedidos, long ticketsAtivos,
                   long ticketsAtrasados, double taxaDeAtraso, long tempoMedioPreparoSegundos) {
    }

    record Hora(int hora, long tickets) {
    }
}
