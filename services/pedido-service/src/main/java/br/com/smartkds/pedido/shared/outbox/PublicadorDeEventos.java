package br.com.smartkds.pedido.shared.outbox;

import br.com.smartkds.pedido.shared.eventos.EventoDeDominio;

/**
 * Porta de publicacao. A implementacao grava o evento na tabela outbox_evento,
 * na MESMA transacao do agregado (ADR-06). Nenhuma fatia fala com o broker.
 */
public interface PublicadorDeEventos {

    void publicar(EventoDeDominio evento);
}
