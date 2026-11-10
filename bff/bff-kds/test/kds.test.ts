import type { ServerResponse } from 'node:http';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { CacheEmMemoria } from '../src/cache/cacheEmMemoria.js';
import { ClienteHttpResiliente } from '../src/clientes/clienteHttpResiliente.js';
import type { EstabelecimentoClient, EstacaoDto } from '../src/clientes/estabelecimento.client.js';
import type { ProducaoClient, TicketDto } from '../src/clientes/producao.client.js';
import { calcularDestaque, TicketsDaEstacaoViewModelBuilder } from '../src/composicao/ticketsDaEstacao.viewmodel.js';
import { ErroHttp, type Contexto } from '../src/contexto.js';
import { rotearEventoParaKds } from '../src/eventos/consumidorDeProducao.js';
import { criarServidor } from '../src/servidor.js';
import { GerenciadorDeStreams } from '../src/sse/gerenciadorDeStreams.js';

const contexto: Contexto = { tenantId: 'demo', usuarioId: 'u', papeis: ['COZINHA'], correlationId: 'c' };

const ticket = (decorrido: number, alvo = 600): TicketDto => ({
  ticketId: `t-${decorrido}`, comandaId: 'c1', mesa: 'Mesa 12', estacaoId: 'grill', status: 'RECEBIDO',
  tempoAlvoSegundos: alvo, decorridoSegundos: decorrido, situacaoSla: 'NO_PRAZO', criadoEm: new Date().toISOString(),
  itens: [{ itemComandaId: 'i1', descricao: 'Hamburguer', quantidade: 2, modificadores: ['Ao ponto'], observacao: 'sem cebola', cancelado: false }],
});

function fakes(estacao: Partial<EstacaoDto> | Error = { percentualAtencao: 50, nome: 'Grill' }) {
  const producao = {
    listarTickets: vi.fn(async () => [ticket(700), ticket(400), ticket(100)]),
    avancarStatus: vi.fn(async () => ({ ticketId: 't', status: 'EM_PREPARO' })),
    expedir: vi.fn(async () => undefined),
  } as unknown as ProducaoClient;
  const estabelecimento = {
    obterEstacao: vi.fn(async () => {
      if (estacao instanceof Error) throw estacao;
      return { codigo: 'grill', ...estacao } as EstacaoDto;
    }),
    listarEstacoes: vi.fn(async () => []),
  } as unknown as EstabelecimentoClient;
  return { producao, estabelecimento };
}

describe('composicao do KDS', () => {
  it('destaque por faixa de atraso (RF15)', () => {
    expect(calcularDestaque(420, 600, 70)).toBe('normal');
    expect(calcularDestaque(421, 600, 70)).toBe('atencao');
    expect(calcularDestaque(601, 600, 70)).toBe('atrasado');
  });

  it('usa o percentual de atencao da estacao e degrada para 70% se a configuracao cair', async () => {
    const { producao, estabelecimento } = fakes();
    const fila = await new TicketsDaEstacaoViewModelBuilder(producao, estabelecimento, new CacheEmMemoria(60_000)).montar(contexto, 'grill');
    expect(fila.tickets.map((t) => t.destaque)).toEqual(['atrasado', 'atencao', 'normal']);

    const semConfig = fakes(new ErroHttp(503, 'x', 'fora'));
    const degradada = await new TicketsDaEstacaoViewModelBuilder(semConfig.producao, semConfig.estabelecimento, new CacheEmMemoria(60_000)).montar(contexto, 'grill');
    expect(degradada.estacao.percentualAtencao).toBe(70);
    expect(degradada.tickets[1].destaque).toBe('normal');
  });
});

describe('rotas', () => {
  it('fila da estacao pelo gateway e recusa sem tenant ou sem papel', async () => {
    const { producao, estabelecimento } = fakes();
    const app = await criarServidor({ producao, estabelecimento, streams: new GerenciadorDeStreams() });

    const ok = await app.inject({ method: 'GET', url: '/estacoes/grill/tickets', headers: { 'x-tenant-id': 'demo', 'x-user-roles': 'COZINHA' } });
    expect(ok.statusCode).toBe(200);
    expect(ok.json().tickets).toHaveLength(3);

    const semTenant = await app.inject({ method: 'GET', url: '/estacoes/grill/tickets' });
    expect(semTenant.statusCode).toBe(401);
    expect(semTenant.headers['content-type']).toContain('application/problem+json');

    const semPapel = await app.inject({ method: 'POST', url: '/tickets/t1/bump', headers: { 'x-tenant-id': 'demo', 'x-user-roles': 'ATENDENTE' } });
    expect(semPapel.statusCode).toBe(403);

    const statusInvalido = await app.inject({ method: 'PATCH', url: '/tickets/t1/status', headers: { 'x-tenant-id': 'demo', 'x-user-roles': 'COZINHA' }, payload: { status: 'EXPEDIDO' } });
    expect(statusInvalido.statusCode).toBe(400);

    const openapi = await app.inject({ method: 'GET', url: '/openapi.json' });
    expect(Object.keys(openapi.json().paths)).toContain('/estacoes/{estacaoId}/tickets');
    await app.close();
  });
});

describe('sse', () => {
  it('evento de ticket chega apenas a estacao certa do tenant certo', () => {
    const streams = new GerenciadorDeStreams(60_000);
    const grill = { write: vi.fn(), end: vi.fn() } as unknown as ServerResponse;
    const bar = { write: vi.fn(), end: vi.fn() } as unknown as ServerResponse;
    const outroTenant = { write: vi.fn(), end: vi.fn() } as unknown as ServerResponse;
    streams.registrar('demo:grill', grill);
    streams.registrar('demo:bar', bar);
    streams.registrar('outro:grill', outroTenant);

    rotearEventoParaKds({ chave: 'ticket.criado', tipo: 'TicketCriado', corpo: { tenantId: 'demo', estacaoId: 'grill', ticketId: 't1' } }, streams);
    rotearEventoParaKds({ chave: 'pedido.item.cancelado', tipo: 'ItemCancelado', corpo: { tenantId: 'demo' } }, streams);

    expect(grill.write).toHaveBeenCalledTimes(2);
    expect(String(vi.mocked(grill.write).mock.calls[0][0])).toContain('event: ticket-criado');
    expect(bar.write).toHaveBeenCalledTimes(1);
    expect(outroTenant.write).not.toHaveBeenCalled();
    streams.fechar();
  });
});

describe('cliente http resiliente', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('abre o circuito apos falhas consecutivas e nao chama mais a dependencia', async () => {
    const fetchFalho = vi.fn(async () => { throw new Error('ECONNREFUSED'); });
    vi.stubGlobal('fetch', fetchFalho);
    const cliente = new ClienteHttpResiliente('producao-service', 'http://x', { limiteFalhasConsecutivas: 2, tentativasLeitura: 1 });

    await expect(cliente.requisitar({ metodo: 'GET', caminho: '/a' })).rejects.toMatchObject({ status: 503 });
    await expect(cliente.requisitar({ metodo: 'GET', caminho: '/a' })).rejects.toMatchObject({ status: 503 });
    expect(cliente.circuitoAberto).toBe(true);
    await expect(cliente.requisitar({ metodo: 'GET', caminho: '/a' })).rejects.toThrow(/circuito aberto/);
    expect(fetchFalho).toHaveBeenCalledTimes(2);
  });

  it('repassa Problem Details 409 do servico sem contar como falha', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify({ title: 'Conflito', detail: 'so e possivel expedir ticket PRONTO' }), { status: 409 })));
    const cliente = new ClienteHttpResiliente('producao-service', 'http://x', { limiteFalhasConsecutivas: 1 });
    await expect(cliente.requisitar({ metodo: 'POST', caminho: '/b' })).rejects.toMatchObject({ status: 409, message: 'so e possivel expedir ticket PRONTO' });
    expect(cliente.circuitoAberto).toBe(false);
  });
});
