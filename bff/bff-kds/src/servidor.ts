import Fastify, { type FastifyInstance } from 'fastify';
import { CacheEmMemoria } from './cache/cacheEmMemoria.js';
import { ClienteHttpResiliente } from './clientes/clienteHttpResiliente.js';
import { EstabelecimentoClient, type EstacaoDto } from './clientes/estabelecimento.client.js';
import { ProducaoClient } from './clientes/producao.client.js';
import { TicketsDaEstacaoViewModelBuilder } from './composicao/ticketsDaEstacao.viewmodel.js';
import { config } from './config.js';
import { ErroHttp } from './contexto.js';
import { registrarDocumentacao } from './docs/documentacao.js';
import { registrarRotasDeStream } from './rotas/stream.routes.js';
import { registrarRotasDeTickets } from './rotas/tickets.routes.js';
import { GerenciadorDeStreams } from './sse/gerenciadorDeStreams.js';

export interface Dependencias {
  producao: ProducaoClient;
  estabelecimento: EstabelecimentoClient;
  streams: GerenciadorDeStreams;
}

export function dependenciasPadrao(): Dependencias {
  return {
    producao: new ProducaoClient(new ClienteHttpResiliente('producao-service', config.producaoUrl)),
    estabelecimento: new EstabelecimentoClient(new ClienteHttpResiliente('estabelecimento-service', config.estabelecimentoUrl)),
    streams: new GerenciadorDeStreams(),
  };
}

/** Composicao do BFF: unico ponto que conhece todas as pecas (equivalente ao HandlerConfig dos servicos). */
export async function criarServidor(deps: Dependencias = dependenciasPadrao()): Promise<FastifyInstance> {
  const app = Fastify({ logger: { level: process.env.LOG_LEVEL ?? 'info' }, genReqId: (r) => String(r.headers['x-correlation-id'] ?? crypto.randomUUID()) });

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
    const validacao = (erro as { validation?: unknown }).validation;
    if (validacao) {
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

  await registrarDocumentacao(app, 'SmartKDS & Menu - bff-kds',
    'Backend for Frontend da tela de producao. Todas as rotas passam pelo API Gateway em /bff/kds/**.', config.prefixoPublico);

  app.get('/health', { schema: { hide: true } }, async () => ({ status: 'UP', conexoesSse: deps.streams.conexoesAtivas() }));

  const cache = new CacheEmMemoria<EstacaoDto>(config.ttlCacheEstacaoMs);
  const composicao = new TicketsDaEstacaoViewModelBuilder(deps.producao, deps.estabelecimento, cache);
  registrarRotasDeTickets(app, { composicao, producao: deps.producao, estabelecimento: deps.estabelecimento });
  registrarRotasDeStream(app, { streams: deps.streams });
  return app;
}
