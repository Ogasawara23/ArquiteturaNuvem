import type { FastifyInstance } from 'fastify';
import { contextoDe } from '../contexto.js';
import { abrirStream, type GerenciadorDeStreams } from '../sse/gerenciadorDeStreams.js';

export function registrarRotasDeStream(app: FastifyInstance, deps: { streams: GerenciadorDeStreams }): void {
  app.get<{ Params: { estacaoId: string } }>('/estacoes/:estacaoId/stream', {
    schema: {
      tags: ['KDS'],
      summary: 'Stream SSE da estacao: ticket-criado, ticket-atualizado, ticket-expedido, fila-alterada (RF16)',
      params: { type: 'object', properties: { estacaoId: { type: 'string' } }, required: ['estacaoId'] },
    },
  }, (req, reply) => {
    const contexto = contextoDe(req);
    const chave = `${contexto.tenantId}:${req.params.estacaoId.toLowerCase()}`;
    reply.hijack();
    abrirStream(reply.raw, contexto.correlationId);
    deps.streams.registrar(chave, reply.raw);
    req.raw.on('close', () => deps.streams.encerrar(chave, reply.raw));
  });
}
