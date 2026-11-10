import type { GerenciadorDeStreams } from '../sse/gerenciadorDeStreams.js';
import type { EventoRecebido, Ligacao } from './consumidorDeEventos.js';

/** Eventos que interessam a tela da cozinha (Secao 8.4). */
export const LIGACOES_KDS: Ligacao[] = [
  { exchange: 'smartkds.producao', chave: 'ticket.*' },
  { exchange: 'smartkds.pedidos', chave: 'pedido.item.cancelado' },
];

const NOMES: Record<string, string> = {
  'ticket.criado': 'ticket-criado',
  'ticket.status': 'ticket-atualizado',
  'ticket.expedido': 'ticket-expedido',
  'pedido.item.cancelado': 'fila-alterada',
};

/**
 * Traduz o evento de dominio em evento SSE e o entrega as conexoes da estacao.
 * ItemCancelado nao carrega a estacao, entao notifica todas as estacoes do tenant.
 */
export function rotearEventoParaKds(evento: EventoRecebido, streams: GerenciadorDeStreams): number {
  const tenantId = String(evento.corpo.tenantId ?? '');
  if (!tenantId) return 0;
  const tipo = NOMES[evento.chave] ?? 'fila-alterada';
  const dados = {
    ticketId: evento.corpo.ticketId,
    estacaoId: evento.corpo.estacaoId,
    status: evento.corpo.status,
    mesa: evento.corpo.mesaRotulo,
    ocorridoEm: evento.corpo.ocorridoEm,
  };
  const id = evento.corpo.eventoId ? String(evento.corpo.eventoId) : undefined;
  const estacaoId = evento.corpo.estacaoId ? String(evento.corpo.estacaoId) : undefined;
  return estacaoId
    ? streams.publicar(`${tenantId}:${estacaoId}`, { tipo, dados, id })
    : streams.publicarParaPrefixo(`${tenantId}:`, { tipo, dados, id });
}
