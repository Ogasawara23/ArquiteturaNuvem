import type { GerenciadorDeStreams } from '../sse/gerenciadorDeStreams.js';
import type { EventoRecebido, Ligacao } from './consumidorDeEventos.js';

/** Tudo que muda o que o cliente ve na comanda (Secao 8.4). */
export const LIGACOES_MESA: Ligacao[] = [
  { exchange: 'smartkds.pedidos', chave: 'pedido.#' },
  { exchange: 'smartkds.pedidos', chave: 'comanda.fechada' },
  { exchange: 'smartkds.producao', chave: 'ticket.status' },
  { exchange: 'smartkds.producao', chave: 'ticket.expedido' },
];

/** O cliente recebe apenas uma notificacao e recarrega a comanda (SSE como canal de notificacao). */
export function rotearEventoParaMesa(evento: EventoRecebido, streams: GerenciadorDeStreams): number {
  const tenantId = evento.corpo.tenantId;
  const comandaId = evento.corpo.comandaId;
  if (!tenantId || !comandaId) return 0;
  return streams.publicar(`${String(tenantId)}:${String(comandaId)}`, {
    tipo: 'comanda-atualizada',
    id: evento.corpo.eventoId ? String(evento.corpo.eventoId) : undefined,
    dados: { motivo: evento.tipo, status: evento.corpo.status ?? null, ocorridoEm: evento.corpo.ocorridoEm ?? null },
  });
}
