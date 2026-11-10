import type { FastifyInstance } from 'fastify';
import type { EstabelecimentoClient } from '../clientes/estabelecimento.client.js';
import type { ProducaoClient } from '../clientes/producao.client.js';
import type { TicketsDaEstacaoViewModelBuilder } from '../composicao/ticketsDaEstacao.viewmodel.js';
import { contextoDe, exigirPapel } from '../contexto.js';

const TAG = ['KDS'];
const ID = { type: 'string', minLength: 1, maxLength: 80 } as const;

export function registrarRotasDeTickets(
  app: FastifyInstance,
  deps: { composicao: TicketsDaEstacaoViewModelBuilder; producao: ProducaoClient; estabelecimento: EstabelecimentoClient },
): void {
  app.get('/estacoes', {
    schema: { tags: TAG, summary: 'Estacoes do estabelecimento, para o seletor do KDS' },
  }, async (req) => deps.estabelecimento.listarEstacoes(contextoDe(req)));

  app.get<{ Params: { estacaoId: string } }>('/estacoes/:estacaoId/tickets', {
    schema: {
      tags: TAG,
      summary: 'Fila da estacao ordenada por urgencia, com destaque normal/atencao/atrasado (RF12, RF15)',
      params: { type: 'object', properties: { estacaoId: ID }, required: ['estacaoId'] },
    },
  }, async (req) => {
    const contexto = contextoDe(req);
    exigirPapel(contexto, 'COZINHA', 'GERENTE', 'ADMIN');
    return deps.composicao.montar(contexto, req.params.estacaoId.toLowerCase());
  });

  app.patch<{ Params: { ticketId: string }; Body: { status: 'EM_PREPARO' | 'PRONTO' } }>('/tickets/:ticketId/status', {
    schema: {
      tags: TAG,
      summary: 'Avanca o ticket: RECEBIDO -> EM_PREPARO -> PRONTO (RF13)',
      params: { type: 'object', properties: { ticketId: ID }, required: ['ticketId'] },
      body: {
        type: 'object',
        properties: { status: { type: 'string', enum: ['EM_PREPARO', 'PRONTO'] } },
        required: ['status'],
      },
    },
  }, async (req) => {
    const contexto = contextoDe(req);
    exigirPapel(contexto, 'COZINHA', 'GERENTE');
    return deps.producao.avancarStatus(contexto, req.params.ticketId, req.body.status);
  });

  app.post<{ Params: { ticketId: string } }>('/tickets/:ticketId/bump', {
    schema: {
      tags: TAG,
      summary: 'Bump: expede o ticket PRONTO e o remove da tela (RF14)',
      params: { type: 'object', properties: { ticketId: ID }, required: ['ticketId'] },
    },
  }, async (req, reply) => {
    const contexto = contextoDe(req);
    exigirPapel(contexto, 'COZINHA', 'GERENTE');
    await deps.producao.expedir(contexto, req.params.ticketId);
    return reply.code(204).send();
  });
}
