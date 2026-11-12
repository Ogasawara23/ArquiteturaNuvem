import { randomUUID } from 'node:crypto';
import Fastify, { type FastifyInstance } from 'fastify';
import { ClienteHttpResiliente } from './clientes/clienteHttpResiliente.js';
import {
  CatalogoClient,
  EstabelecimentoClient,
  PedidoClient,
  ProducaoClient,
  StatusOperacionalClient,
} from './clientes/servicos.client.js';
import { DadosAgregadosComposicao } from './composicao/dadosAgregados.js';
import { config } from './config.js';
import { ErroHttp } from './contexto.js';
import { registrarDocumentacao } from './docs/documentacao.js';
import { registrarDocumentacaoDeEventos } from './docs/eventos.js';
import { registrarRotasDeAdministracao } from './rotas/administracao.routes.js';
import { registrarRotasDeAgregacao } from './rotas/agregacao.routes.js';

export interface Dependencias {
  catalogo: CatalogoClient;
  pedido: PedidoClient;
  producao: ProducaoClient;
  estabelecimento: EstabelecimentoClient;
  statusOperacional: StatusOperacionalClient;
}

export function dependenciasPadrao(): Dependencias {
  return {
    catalogo: new CatalogoClient(new ClienteHttpResiliente('catalogo-service', config.catalogoUrl)),
    pedido: new PedidoClient(new ClienteHttpResiliente('pedido-service', config.pedidoUrl)),
    producao: new ProducaoClient(new ClienteHttpResiliente('producao-service', config.producaoUrl)),
    estabelecimento: new EstabelecimentoClient(new ClienteHttpResiliente('estabelecimento-service', config.estabelecimentoUrl)),
    statusOperacional: new StatusOperacionalClient(config.functionStatusUrl, config.functionKey),
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

  await registrarDocumentacao(app, 'SmartKDS & Menu - bff-admin',
    'Backend for Frontend das telas administrativas (gerente/atendente). Exposto em /bff/admin/** pelo API Gateway. '
    + 'Contrato dos eventos assincronos: ./eventos (AsyncAPI).', config.prefixoPublico);
  registrarDocumentacaoDeEventos(app, config.arquivoAsyncApi);
  app.get('/health', { schema: { hide: true } }, async () => ({ status: 'UP' }));

  registrarRotasDeAgregacao(app, {
    agregacao: new DadosAgregadosComposicao(deps.catalogo, deps.pedido, deps.statusOperacional, deps.producao),
  });
  registrarRotasDeAdministracao(app, deps);
  return app;
}
