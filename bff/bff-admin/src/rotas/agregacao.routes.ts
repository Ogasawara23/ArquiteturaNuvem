import type { FastifyInstance } from 'fastify';
import type { DadosAgregadosComposicao } from '../composicao/dadosAgregados.js';
import { contextoDe, exigirPapel } from '../contexto.js';

export function registrarRotasDeAgregacao(app: FastifyInstance, deps: { agregacao: DadosAgregadosComposicao }): void {
  app.get('/aggregated-data', {
    schema: {
      tags: ['Agregacao'],
      summary: 'Agrega Microsservico 1 (catalogo), Microsservico 2 (pedidos) e Azure Function em um unico response',
      description: 'Chamadas em paralelo, com timeout e circuit breaker por fonte. Fontes indisponiveis aparecem em `fontes` e o restante continua sendo entregue.',
      response: {
        200: {
          type: 'object',
          properties: {
            tenantId: { type: 'string' },
            geradoEm: { type: 'string', format: 'date-time' },
            fontes: {
              type: 'array',
              items: {
                type: 'object',
                properties: {
                  nome: { type: 'string' }, tipo: { type: 'string' }, situacao: { type: 'string', enum: ['ok', 'indisponivel'] },
                  latenciaMs: { type: 'number' }, erro: { type: 'string' },
                },
              },
            },
            catalogo: { type: ['object', 'null'], additionalProperties: true },
            pedidos: { type: ['object', 'null'], additionalProperties: true },
            statusOperacional: { type: ['object', 'null'], additionalProperties: true },
            producao: { type: ['object', 'null'], additionalProperties: true },
            estimativa: { type: 'object', additionalProperties: true },
          },
        },
      },
    },
  }, async (req) => {
    const contexto = contextoDe(req);
    exigirPapel(contexto, 'GERENTE', 'ADMIN', 'ATENDENTE');
    return deps.agregacao.montar(contexto);
  });
}
