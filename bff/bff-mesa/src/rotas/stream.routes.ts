import { randomUUID } from 'node:crypto';
import type { FastifyInstance } from 'fastify';
import type { MesaComposicao } from '../composicao/mesa.composicao.js';
import { abrirStream, type GerenciadorDeStreams } from '../sse/gerenciadorDeStreams.js';

export function registrarRotasDeStream(app: FastifyInstance, deps: { composicao: MesaComposicao; streams: GerenciadorDeStreams }): void {
  app.get<{ Params: { qrToken: string } }>('/:qrToken/comanda/stream', {
    schema: {
      tags: ['Mesa (cliente)'],
      summary: 'Stream SSE da comanda aberta: evento comanda-atualizada a cada mudanca no pedido ou na producao (RF16)',
      params: { type: 'object', properties: { qrToken: { type: 'string' } }, required: ['qrToken'] },
    },
  }, async (req, reply) => {
    const correlationId = String(req.headers['x-correlation-id'] ?? randomUUID());
    const cliente = await deps.composicao.resolver(req.params.qrToken, correlationId);
    const comanda = await deps.composicao.exigirComandaAberta(cliente);
    const chave = `${cliente.mesa.tenantId}:${comanda.comandaId}`;
    reply.hijack();
    abrirStream(reply.raw, correlationId);
    deps.streams.registrar(chave, reply.raw);
    req.raw.on('close', () => deps.streams.encerrar(chave, reply.raw));
  });
}
