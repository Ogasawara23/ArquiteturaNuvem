import type { FastifyInstance, FastifyRequest } from 'fastify';
import type { CatalogoClient, EstabelecimentoClient, PedidoClient, ProducaoClient, Repassador } from '../clientes/servicos.client.js';
import { contextoDe, exigirPapel } from '../contexto.js';

type Metodo = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

interface Rota {
  metodo: Metodo;
  url: string;
  destino: (p: Record<string, string>) => string;
  servico: Repassador;
  papeis: string[];
  tag: string;
  resumo: string;
  status?: number;
}

const GERENCIA = ['GERENTE', 'ADMIN'];
const SALAO = ['ATENDENTE', 'GERENTE', 'ADMIN'];

/**
 * Proxy das operacoes CRUD administrativas (RF02, RF03, RF10, RF18, RF19, RF20).
 * Cada rota publica corresponde a uma fatia de um microsservico (matriz de rastreabilidade, Secao 1.1).
 */
export function registrarRotasDeAdministracao(
  app: FastifyInstance,
  s: { catalogo: CatalogoClient; pedido: PedidoClient; producao: ProducaoClient; estabelecimento: EstabelecimentoClient },
): void {
  const rotas: Rota[] = [
    // catalogo-service (MongoDB)
    { metodo: 'GET', url: '/cardapio/itens', destino: () => '/interno/itens', servico: s.catalogo, papeis: GERENCIA, tag: 'Cardapio', resumo: 'Lista os itens em edicao' },
    { metodo: 'POST', url: '/cardapio/itens', destino: () => '/interno/itens', servico: s.catalogo, papeis: GERENCIA, tag: 'Cardapio', resumo: 'Cadastra item (cadastraritem)', status: 201 },
    { metodo: 'GET', url: '/cardapio/itens/:itemId', destino: (p) => `/interno/itens/${p.itemId}`, servico: s.catalogo, papeis: GERENCIA, tag: 'Cardapio', resumo: 'Detalha item' },
    { metodo: 'PUT', url: '/cardapio/itens/:itemId', destino: (p) => `/interno/itens/${p.itemId}`, servico: s.catalogo, papeis: GERENCIA, tag: 'Cardapio', resumo: 'Atualiza item (atualizaritem)' },
    { metodo: 'PATCH', url: '/cardapio/itens/:itemId/disponibilidade', destino: (p) => `/interno/itens/${p.itemId}/disponibilidade`, servico: s.catalogo, papeis: GERENCIA, tag: 'Cardapio', resumo: 'Disponivel/esgotado (alterardisponibilidade)' },
    { metodo: 'DELETE', url: '/cardapio/itens/:itemId', destino: (p) => `/interno/itens/${p.itemId}`, servico: s.catalogo, papeis: GERENCIA, tag: 'Cardapio', resumo: 'Remove item (removeritem)', status: 204 },
    { metodo: 'POST', url: '/cardapio/versoes', destino: () => '/interno/cardapios/versoes', servico: s.catalogo, papeis: GERENCIA, tag: 'Cardapio', resumo: 'Publica versao imutavel (publicarversaocardapio)', status: 201 },
    { metodo: 'GET', url: '/cardapio/ativo', destino: () => '/interno/cardapios/ativo', servico: s.catalogo, papeis: GERENCIA, tag: 'Cardapio', resumo: 'Versao ativa do cardapio' },
    // pedido-service (Azure SQL)
    { metodo: 'GET', url: '/comandas', destino: () => '/interno/comandas', servico: s.pedido, papeis: SALAO, tag: 'Comandas', resumo: 'Lista comandas (?status=ABERTA|FECHADA)' },
    { metodo: 'GET', url: '/comandas/:comandaId', destino: (p) => `/interno/comandas/${p.comandaId}`, servico: s.pedido, papeis: SALAO, tag: 'Comandas', resumo: 'Detalha comanda' },
    { metodo: 'POST', url: '/comandas', destino: () => '/interno/comandas', servico: s.pedido, papeis: SALAO, tag: 'Comandas', resumo: 'Abre (ou reaproveita) a comanda de uma mesa (abrircomanda)' },
    { metodo: 'POST', url: '/comandas/:comandaId/itens', destino: (p) => `/interno/comandas/${p.comandaId}/itens`, servico: s.pedido, papeis: SALAO, tag: 'Comandas', resumo: 'Adiciona item (adicionaritem)', status: 201 },
    { metodo: 'PATCH', url: '/comandas/:comandaId/itens/:itemId', destino: (p) => `/interno/comandas/${p.comandaId}/itens/${p.itemId}`, servico: s.pedido, papeis: SALAO, tag: 'Comandas', resumo: 'Altera quantidade/observacao de item no carrinho (alteraritemdacomanda)' },
    { metodo: 'DELETE', url: '/comandas/:comandaId/itens/:itemId', destino: (p) => `/interno/comandas/${p.comandaId}/itens/${p.itemId}`, servico: s.pedido, papeis: SALAO, tag: 'Comandas', resumo: 'Cancela item (cancelaritem)', status: 204 },
    { metodo: 'POST', url: '/comandas/:comandaId/confirmacao', destino: (p) => `/interno/comandas/${p.comandaId}/confirmacao`, servico: s.pedido, papeis: SALAO, tag: 'Comandas', resumo: 'Envia os itens pendentes para a cozinha (confirmarpedido)' },
    { metodo: 'DELETE', url: '/comandas/:comandaId', destino: (p) => `/interno/comandas/${p.comandaId}`, servico: s.pedido, papeis: SALAO, tag: 'Comandas', resumo: 'Cancela a comanda (cancelarcomanda)', status: 204 },
    { metodo: 'POST', url: '/comandas/:comandaId/fechamento', destino: (p) => `/interno/comandas/${p.comandaId}/fechamento`, servico: s.pedido, papeis: SALAO, tag: 'Comandas', resumo: 'Fecha a comanda com taxa de servico (fecharcomanda, RF10)' },
    { metodo: 'GET', url: '/auditoria', destino: () => '/interno/auditoria', servico: s.pedido, papeis: GERENCIA, tag: 'Auditoria', resumo: 'Trilha de auditoria (consultarauditoria, RF20)' },
    // producao-service
    { metodo: 'GET', url: '/indicadores', destino: () => '/interno/indicadores', servico: s.producao, papeis: GERENCIA, tag: 'Indicadores', resumo: 'Indicadores operacionais (consultarindicadores, RF18)' },
    // estabelecimento-service
    { metodo: 'GET', url: '/estabelecimento', destino: () => '/interno/estabelecimento', servico: s.estabelecimento, papeis: GERENCIA, tag: 'Estabelecimento', resumo: 'Configuracao do estabelecimento' },
    { metodo: 'PUT', url: '/estabelecimento', destino: () => '/interno/estabelecimento', servico: s.estabelecimento, papeis: GERENCIA, tag: 'Estabelecimento', resumo: 'Provisiona/atualiza o estabelecimento' },
    { metodo: 'GET', url: '/mesas', destino: () => '/interno/mesas', servico: s.estabelecimento, papeis: SALAO, tag: 'Estabelecimento', resumo: 'Mesas e QR tokens' },
    { metodo: 'POST', url: '/mesas', destino: () => '/interno/mesas', servico: s.estabelecimento, papeis: GERENCIA, tag: 'Estabelecimento', resumo: 'Cadastra mesa (cadastrarmesa, RF19)', status: 201 },
    { metodo: 'POST', url: '/mesas/:mesaId/qr-token', destino: (p) => `/interno/mesas/${p.mesaId}/qr-token`, servico: s.estabelecimento, papeis: GERENCIA, tag: 'Estabelecimento', resumo: 'Rotaciona o QR Code da mesa' },
    { metodo: 'GET', url: '/estacoes', destino: () => '/interno/estacoes', servico: s.estabelecimento, papeis: GERENCIA, tag: 'Estabelecimento', resumo: 'Estacoes de producao' },
    { metodo: 'POST', url: '/estacoes', destino: () => '/interno/estacoes', servico: s.estabelecimento, papeis: GERENCIA, tag: 'Estabelecimento', resumo: 'Cadastra estacao (cadastrarestacao, RF19)', status: 201 },
    { metodo: 'GET', url: '/roteamento', destino: () => '/interno/roteamento', servico: s.estabelecimento, papeis: GERENCIA, tag: 'Estabelecimento', resumo: 'Regras de roteamento item -> estacao' },
    { metodo: 'PUT', url: '/roteamento', destino: () => '/interno/roteamento', servico: s.estabelecimento, papeis: GERENCIA, tag: 'Estabelecimento', resumo: 'Configura roteamento (configurarroteamento, RF19)', status: 204 },
  ];

  for (const rota of rotas) {
    app.route({
      method: rota.metodo,
      url: rota.url,
      schema: { tags: [rota.tag], summary: rota.resumo },
      handler: async (req: FastifyRequest, reply) => {
        const contexto = contextoDe(req);
        exigirPapel(contexto, ...rota.papeis);
        const params = (req.params ?? {}) as Record<string, string>;
        const consulta = (req.query ?? {}) as Record<string, string>;
        const resposta = await rota.servico.repassar(contexto, rota.metodo, rota.destino(params), req.body ?? undefined, consulta);
        return rota.status === 204 ? reply.code(204).send() : reply.code(rota.status ?? 200).send(resposta);
      },
    });
  }
}
