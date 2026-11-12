import { randomUUID } from 'node:crypto';
import Fastify, { type FastifyInstance } from 'fastify';
import { CacheEmMemoria } from './cache/cacheEmMemoria.js';
import { ClienteHttpResiliente } from './clientes/clienteHttpResiliente.js';
import {
  CatalogoClient,
  EstabelecimentoClient,
  PedidoClient,
  type CardapioAtivo,
  type MesaResolvida,
} from './clientes/servicos.client.js';
import { MesaComposicao } from './composicao/mesa.composicao.js';
import { config } from './config.js';
import { ErroHttp } from './contexto.js';
import { registrarDocumentacao } from './docs/documentacao.js';
import { registrarRotasDaMesa } from './rotas/mesa.routes.js';
import { registrarRotasDeStream } from './rotas/stream.routes.js';
import { GerenciadorDeStreams } from './sse/gerenciadorDeStreams.js';

export interface Dependencias {
  estabelecimento: EstabelecimentoClient;
  catalogo: CatalogoClient;
  pedido: PedidoClient;
  streams: GerenciadorDeStreams;
}

export function dependenciasPadrao(): Dependencias {
  return {
    estabelecimento: new EstabelecimentoClient(new ClienteHttpResiliente('estabelecimento-service', config.estabelecimentoUrl)),
    catalogo: new CatalogoClient(new ClienteHttpResiliente('catalogo-service', config.catalogoUrl)),
    pedido: new PedidoClient(new ClienteHttpResiliente('pedido-service', config.pedidoUrl)),
    streams: new GerenciadorDeStreams(),
  };
}

export async function criarServidor(deps: Dependencias = dependenciasPadrao()): Promise<FastifyInstance> {
  const app = Fastify({ logger: { level: process.env.LOG_LEVEL ?? 'info' }, genReqId: (r) => String(r.headers['x-correlation-id'] ?? randomUUID()) });

  // Corpo JSON vazio (ex.: POST /bump enviado com content-type) e aceito como "sem corpo".
  app.removeContentTypeParser('application/json');
  app.addContentTypeParser('application/json', { parseAs: 'string' }, (_req, corpo, pronto) => {
    const texto = String(corpo ?? '').trim();
    if (!texto) return pronto(null, undefined);
    try {
      pronto(null, JSON.parse(texto));
    } catch {
      pronto(new ErroHttp(400, 'Requisicao invalida', 'JSON malformado'), undefined);
    }
  });

  app.setErrorHandler((erro, req, reply) => {
    const correlationId = String(req.headers['x-correlation-id'] ?? req.id);
    if (erro instanceof ErroHttp) {
      return reply.code(erro.status).type('application/problem+json')
        .send({ title: erro.titulo, status: erro.status, detail: erro.message, correlationId });
    }
    if ((erro as { validation?: unknown }).validation) {
      return reply.code(400).type('application/problem+json')
        .send({ title: 'Requisicao invalida', status: 400, detail: (erro as Error).message, correlationId });
    }
    const statusDoFastify = (erro as { statusCode?: number }).statusCode;
    if (statusDoFastify && statusDoFastify >= 400 && statusDoFastify < 500) {
      return reply.code(statusDoFastify).type('application/problem+json')
        .send({ title: 'Requisicao invalida', status: statusDoFastify, detail: (erro as Error).message, correlationId });
    }
    req.log.error(erro);
    return reply.code(500).type('application/problem+json')
      .send({ title: 'Erro interno', status: 500, detail: 'erro inesperado', correlationId });
  });

  await registrarDocumentacao(app, 'SmartKDS & Menu - bff-mesa',
    'Backend for Frontend do cliente na mesa (sem login: a credencial e o qrToken). Exposto em /bff/mesa/** pelo API Gateway.',
    config.prefixoPublico);
  app.get('/health', { schema: { hide: true } }, async () => ({ status: 'UP', conexoesSse: deps.streams.conexoesAtivas() }));

  const composicao = new MesaComposicao(deps.estabelecimento, deps.catalogo, deps.pedido,
    new CacheEmMemoria<MesaResolvida>(config.ttlCacheMesaMs), new CacheEmMemoria<CardapioAtivo>(config.ttlCacheCardapioMs));
  registrarRotasDaMesa(app, { composicao });
  registrarRotasDeStream(app, { composicao, streams: deps.streams });
  return app;
}
