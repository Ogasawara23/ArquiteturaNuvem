package br.com.smartkds.pedido.shared.outbox;

import br.com.smartkds.pedido.shared.eventos.EventoDeDominio;
import br.com.smartkds.pedido.shared.tenancy.ContextoDoTenant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
public class PublicadorDeEventosOutbox implements PublicadorDeEventos {

    private final OutboxSpringData outbox;
    private final ObjectMapper json;
    private final Clock relogio;

    PublicadorDeEventosOutbox(OutboxSpringData outbox, ObjectMapper json, Clock relogio) {
        this.outbox = outbox;
        this.json = json;
        this.relogio = relogio;
    }

    /** MANDATORY: falha se for chamado fora da transacao do agregado. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publicar(EventoDeDominio evento) {
        try {
            outbox.save(new RegistroOutboxEntity(evento.eventoId(), evento.tenantId(), evento.tipo(),
                    evento.routingKey(), json.writeValueAsString(evento), ContextoDoTenant.traceparent(),
                    Instant.now(relogio)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("evento nao serializavel: " + evento.tipo(), e);
        }
    }
}
