import { describe, expect, it, vi } from 'vitest';
import type {
  CatalogoClient,
  EstabelecimentoClient,
  PedidoClient,
  ProducaoClient,
  StatusOperacionalClient,
} from '../src/clientes/servicos.client.js';
import { estimar, resumirPedidos } from '../src/composicao/dadosAgregados.js';
import { ErroHttp } from '../src/contexto.js';
import { criarServidor } from '../src/servidor.js';

const CABECALHOS = { 'x-tenant-id': 'demo', 'x-user-roles': 'GERENTE', 'x-correlation-id': 'corr-1' };

function deps(opcoes: { functionFora?: boolean } = {}) {
  const catalogo = {
    listarItens: vi.fn(async () => [
      { id: '1', nome: 'Hamburguer', categoria: 'Lanches', precoCentavos: 4200, estacaoSugerida: 'grill', tempoAlvoPreparoSegundos: 720, disponivel: true },
      { id: '2', nome: 'Suco', categoria: 'Bebidas', precoCentavos: 1200, estacaoSugerida: 'bar', tempoAlvoPreparoSegundos: 120, disponivel: false },
    ]),
    repassar: vi.fn(async () => ({ id: 'novo' })),
  } as unknown as CatalogoClient;
  const pedido = {
    listarComandas: vi.fn(async () => [
      { comandaId: 'c1', mesaRotulo: 'Mesa 1', status: 'ABERTA', subtotalCentavos: 5000, totalCentavos: null, abertaEm: '2026-10-15T15:00:00Z', fechadaEm: null, itens: [{ status: 'CONFIRMADO', quantidade: 2 }] },
    ]),
    repassar: vi.fn(async () => ({})),
  } as unknown as PedidoClient;
  const producao = {
    indicadores: vi.fn(async () => ({ desde: '', volumePorHora: [], estacoes: [{ estacaoId: 'grill', ticketsCriados: 3, ticketsExpedidos: 2, ticketsAtivos: 1, ticketsAtrasados: 1, taxaDeAtraso: 0.5, tempoMedioPreparoSegundos: 600 }] })),
    repassar: vi.fn(),
  } as unknown as ProducaoClient;
  const statusOperacional = {
    consultar: vi.fn(async () => {
      if (opcoes.functionFora) throw new ErroHttp(503, 'x', 'Azure Function indisponivel');
      return { faixaHoraria: 'jantar', emHorarioDePico: true, multiplicadorTempoPreparo: 1.3, mensagem: '', horaLocal: '20:00', diaDaSemana: 'sexta' };
    }),
  } as unknown as StatusOperacionalClient;
  const estabelecimento = { repassar: vi.fn(async () => []) } as unknown as EstabelecimentoClient;
  return { catalogo, pedido, producao, statusOperacional, estabelecimento };
}

describe('GET /aggregated-data', () => {
  it('consome os dois microsservicos e a Azure Function e devolve um unico response', async () => {
    const d = deps();
    const app = await criarServidor(d);
    const r = await app.inject({ method: 'GET', url: '/aggregated-data', headers: CABECALHOS });
    expect(r.statusCode).toBe(200);
    const corpo = r.json();
    expect(corpo.fontes.map((f: { nome: string }) => f.nome).sort()).toEqual(
      ['azure-function:status-operacional', 'catalogo-service', 'pedido-service', 'producao-service']);
    expect(corpo.catalogo).toMatchObject({ totalDeItens: 2, itensDisponiveis: 1, itensEsgotados: 1 });
    expect(corpo.pedidos).toMatchObject({ comandasAbertas: 1, valorEmAbertoCentavos: 5000, itensAguardandoCozinha: 2 });
    expect(corpo.statusOperacional.emHorarioDePico).toBe(true);
    expect(corpo.estimativa.tempoEstimadoComPicoSegundos).toBe(780);
    expect(vi.mocked(d.catalogo.listarItens).mock.calls[0][0]).toMatchObject({ tenantId: 'demo', correlationId: 'corr-1' });
    await app.close();
  });

  it('uma fonte fora nao derruba a agregacao', async () => {
    const app = await criarServidor(deps({ functionFora: true }));
    const corpo = (await app.inject({ method: 'GET', url: '/aggregated-data', headers: CABECALHOS })).json();
    expect(corpo.statusOperacional).toBeNull();
    expect(corpo.fontes.find((f: { nome: string }) => f.nome.startsWith('azure')).situacao).toBe('indisponivel');
    expect(corpo.catalogo).not.toBeNull();
    await app.close();
  });

  it('exige tenant e papel', async () => {
    const app = await criarServidor(deps());
    expect((await app.inject({ method: 'GET', url: '/aggregated-data' })).statusCode).toBe(401);
    expect((await app.inject({ method: 'GET', url: '/aggregated-data', headers: { 'x-tenant-id': 'demo', 'x-user-roles': 'COZINHA' } })).statusCode).toBe(403);
    await app.close();
  });
});

describe('proxy CRUD', () => {
  it('repassa o cadastro de item ao catalogo-service com o contexto do gateway', async () => {
    const d = deps();
    const app = await criarServidor(d);
    const r = await app.inject({ method: 'POST', url: '/cardapio/itens', headers: CABECALHOS, payload: { nome: 'X' } });
    expect(r.statusCode).toBe(201);
    expect(d.catalogo.repassar).toHaveBeenCalledWith(expect.objectContaining({ tenantId: 'demo' }), 'POST', '/interno/itens', { nome: 'X' }, {});
    const atendenteNaoPublica = await app.inject({ method: 'POST', url: '/cardapio/versoes', headers: { ...CABECALHOS, 'x-user-roles': 'ATENDENTE' } });
    expect(atendenteNaoPublica.statusCode).toBe(403);
    await app.close();
  });

  it('documentacao OpenAPI lista o endpoint agregado', async () => {
    const app = await criarServidor(deps());
    const paths = Object.keys((await app.inject({ method: 'GET', url: '/openapi.json' })).json().paths);
    expect(paths).toContain('/aggregated-data');
    expect(paths).toContain('/cardapio/itens/{itemId}');
    await app.close();
  });
});

describe('regras de composicao', () => {
  it('faturamento considera apenas comandas fechadas nas ultimas 24h', () => {
    const agora = new Date('2026-10-16T12:00:00Z');
    const r = resumirPedidos([
      { comandaId: 'a', mesaRotulo: '1', status: 'FECHADA', subtotalCentavos: 1000, totalCentavos: 1100, abertaEm: '', fechadaEm: '2026-10-16T10:00:00Z', itens: [] },
      { comandaId: 'b', mesaRotulo: '2', status: 'FECHADA', subtotalCentavos: 9000, totalCentavos: 9900, abertaEm: '', fechadaEm: '2026-10-14T10:00:00Z', itens: [] },
    ], agora);
    expect(r.faturamentoNasUltimas24hCentavos).toBe(1100);
  });

  it('estimativa sem dados de producao e explicita', () => {
    expect(estimar(null, null).tempoMedioPreparoSegundos).toBeNull();
  });
});
