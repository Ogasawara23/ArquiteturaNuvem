package br.com.smartkds.producao.features.registrareventorejeitado;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registra telemetria da mensagem rejeitada: metrica eventos_rejeitados_total (base do
 * alerta imediato no Grafana) e log estruturado com o traceparent para investigacao.
 * A mensagem e consumida (ack) e fica registrada; o reprocessamento e manual via shovel.
 */
@Component
class RegistrarEventoRejeitadoHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrarEventoRejeitadoHandler.class);

    private final MeterRegistry metricas;

    RegistrarEventoRejeitadoHandler(MeterRegistry metricas) {
        this.metricas = metricas;
    }

    public void executar(RegistrarEventoRejeitadoCommand comando) {
        metricas.counter("eventos_rejeitados_total", "fila", comando.fila()).increment();
        LOG.error("EVENTO REJEITADO fila={} eventoId={} tipo={} traceparent={} corpo={}",
                comando.fila(), comando.eventoId(), comando.tipo(), comando.traceparent(), comando.trechoDoCorpo());
    }
}
