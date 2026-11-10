import { randomUUID } from 'node:crypto';
import type { FastifyInstance, FastifyRequest } from 'fastify';
import type { MesaComposicao, NovoItem } from '../composicao/mesa.composicao.js';

const TAG = ['Mesa (cliente)'];
const TOKEN = { type: 'object', properties: { qrToken: { type: 'string', minLength: 6, maxLength: 120 } }, required: ['qrToken'] } as const;

const correlacao = (req: FastifyRequest) => String(req.headers['x-correlation-id'] ?? randomUUID());

/** Rotas publicas: a credencial e o qrToken, nunca um JWT (Secao 8.3). */
export function registrarRotasDaMesa(app: FastifyInstance, deps: { composicao: MesaComposicao }): void {
  const { composicao } = deps;

  app.get<{ Params: { qrToken: string } }>('/:qrToken/cardapio', {
    schema: { tags: TAG, summary: 'Cardapio ativo + comanda aberta da mesa em uma unica resposta (RF04, RF09)', params: TOKEN },
  }, async (req) => composicao.tela(req.params.qrToken, correlacao(req)));

  app.get<{ Params: { qrToken: string } }>('/:qrToken/comanda', {
    schema: { tags: TAG, summary: 'Estado atual da comanda e de cada item (RF09)', params: TOKEN },
  }, async (req, reply) => {
    const cliente = await composicao.resolver(req.params.qrToken, correlacao(req));
    const comanda = await composicao.comandaAberta(cliente.contexto, cliente.mesa.mesaId);
    return comanda ?? reply.code(204).send();
  });

  app.post<{ Params: { qrToken: string } }>('/:qrToken/comanda', {
    schema: { tags: TAG, summary: 'Abre (ou reaproveita) a comanda da mesa (RF05)', params: TOKEN },
  }, async (req, reply) => {
    const cliente = await composicao.resolver(req.params.qrToken, correlacao(req));
    return reply.code(201).send(await composicao.abrirComanda(cliente));
  });

  app.post<{ Params: { qrToken: string }; Body: NovoItem }>('/:qrToken/comanda/itens', {
    schema: {
      tags: TAG,
      summary: 'Adiciona item ao carrinho; abre a comanda se necessario (RF05, RF06)',
      params: TOKEN,
      body: {
        type: 'object',
        required: ['itemCardapioId', 'quantidade'],
        properties: {
          itemCardapioId: { type: 'string', minLength: 1 },
          quantidade: { type: 'integer', minimum: 1, maximum: 99 },
          observacao: { type: 'string', maxLength: 200 },
          modificadores: {
            type: 'array',
            items: { type: 'object', required: ['grupo', 'opcao'], properties: { grupo: { type: 'string' }, opcao: { type: 'string' } } },
          },
        },
      },
    },
  }, async (req, reply) => {
    const cliente = await composicao.resolver(req.params.qrToken, correlacao(req));
    return reply.code(201).send(await composicao.adicionarItem(cliente, req.body));
  });

  app.post<{ Params: { qrToken: string }; Body: { chaveIdempotencia: string } }>('/:qrToken/comanda/confirmacao', {
    schema: {
      tags: TAG,
      summary: 'Envia os itens pendentes para a cozinha (RF07). Reenvio com a mesma chave nao duplica o pedido.',
      params: TOKEN,
      body: { type: 'object', required: ['chaveIdempotencia'], properties: { chaveIdempotencia: { type: 'string', minLength: 1, maxLength: 64 } } },
    },
  }, async (req) => {
    const cliente = await composicao.resolver(req.params.qrToken, correlacao(req));
    return composicao.confirmar(cliente, req.body.chaveIdempotencia);
  });

  app.delete<{ Params: { qrToken: string; itemId: string } }>('/:qrToken/comanda/itens/:itemId', {
    schema: {
      tags: TAG,
      summary: 'Cancela item ainda nao iniciado pela cozinha (RF08)',
      params: { type: 'object', properties: { qrToken: { type: 'string' }, itemId: { type: 'string' } }, required: ['qrToken', 'itemId'] },
    },
  }, async (req, reply) => {
    const cliente = await composicao.resolver(req.params.qrToken, correlacao(req));
    await composicao.cancelarItem(cliente, req.params.itemId);
    return reply.code(204).send();
  });
}
