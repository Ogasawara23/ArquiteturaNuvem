package br.com.smartkds.producao.shared.inbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbox: registro dos eventos ja consumidos. Consultado antes de qualquer efeito e
 * marcado na MESMA transacao do efeito (Secao 8.4, idempotencia do consumo).
 */
public interface EventoProcessadoRepository {

    boolean jaProcessado(UUID eventoId);

    void marcarProcessado(UUID eventoId, String origem, Instant agora);
}
