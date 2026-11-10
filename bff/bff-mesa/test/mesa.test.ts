import type { ServerResponse } from 'node:http';
import { describe, expect, it, vi } from 'vitest';
import type { CatalogoClient, Comanda, EstabelecimentoClient, PedidoClient } from '../src/clientes/servicos.client.js';
import { ErroHttp } from '../src/contexto.js';
import { rotearEventoParaMesa } from '../src/eventos/consumidorDaMesa.js';
import { criarServidor } from '../src/servidor.js';
import { GerenciadorDeStreams } from '../src/sse/gerenciadorDeStreams.js';

const comanda: Comanda = { comandaId: 'c-1', mesaId: 'm-1', mesaRotulo: 'Mesa 1', status: 'ABERTA', subtotalCentavos: 4200, itens: [] };

function cenario(opcoes: { pedidoFora?: boolean; comandaAberta?: boolean } = {}) {
  const estabelecimento = {
    resolverQrToken: vi.fn(async (token: string) => {
      if (token !== 'demo-mesa-1') throw new ErroHttp(404, 'x', 'nao encontrado');
      return { tenantId: 'demo', mesaId: 'm-1', rotulo: 'Mesa 1', nomeEstabelecimento: 'Trattoria', percentualTaxaServico: 10 };
    }),
  } as unknown as EstabelecimentoClient;
  const catalogo = {
    cardapioAtivo: vi.fn(async () => ({ versaoId: 'v-1', publicadaEm: '', categorias: [{ nome: 'Lanches', itens: [] }] })),
  } as unknown as CatalogoClient;
  let aberta = opcoes.comandaAberta ? comanda : undefined;
  const pedido = {
    comandasAbertasDaMesa: vi.fn(async () => {
      if (opcoes.pedidoFora) throw new ErroHttp(503, 'x', 'pedido-service indisponivel');
      return aberta ? [aberta] : [];
    }),
    abrir: vi.fn(async () => { aberta = comanda; return { comandaId: 'c-1', reaproveitada: false }; }),
    obter: vi.fn(async () => comanda),
    adicionarItem: vi.fn(async () => ({})),
    confirmar: vi.fn(async () => ({ repeticao: false })),
    cancelarItem: vi.fn(async () => undefined),
  } as unknown as PedidoClient;
  return { estabelecimento, catalogo, pedido, streams: new GerenciadorDeStreams(60_000) };
}

describe('bff-mesa', () => {
  it('compoe cardapio e comanda em uma unica resposta', async () => {
    const deps = cenario({ comandaAberta: true });
    const app = await criarServidor(deps);
    const r = await app.inject({ method: 'GET', url: '/demo-mesa-1/cardapio' });
    expect(r.statusCode).toBe(200);
    expect(r.json()).toMatchObject({ mesa: { rotulo: 'Mesa 1' }, cardapio: { versaoId: 'v-1' }, comanda: { comandaId: 'c-1' }, avisos: [] });
    // o contexto interno usa o tenant resolvido pelo token, nunca um cabecalho do cliente
    expect(vi.mocked(deps.catalogo.cardapioAtivo).mock.calls[0][0].tenantId).toBe('demo');
    await app.close();
  });

  it('degradacao parcial: pedido-service fora nao derruba o cardapio (Secao 8.6)', async () => {
    const app = await criarServidor(cenario({ pedidoFora: true }));
    const r = await app.inject({ method: 'GET', url: '/demo-mesa-1/cardapio' });
    expect(r.statusCode).toBe(200);
    expect(r.json().cardapio).not.toBeNull();
    expect(r.json().avisos).toContain('estado da comanda temporariamente indisponivel');
    await app.close();
  });

  it('qrToken invalido responde 404 com mensagem para o cliente', async () => {
    const app = await criarServidor(cenario());
    const r = await app.inject({ method: 'GET', url: '/token-falso-123/cardapio' });
    expect(r.statusCode).toBe(404);
    expect(r.json().detail).toBe('QR Code invalido, chame o atendente');
    await app.close();
  });

  it('primeiro item abre a comanda automaticamente (Figura 14)', async () => {
    const deps = cenario();
    const app = await criarServidor(deps);
    const r = await app.inject({ method: 'POST', url: '/demo-mesa-1/comanda/itens', payload: { itemCardapioId: 'x', quantidade: 2 } });
    expect(r.statusCode).toBe(201);
    expect(deps.pedido.abrir).toHaveBeenCalledOnce();
    expect(deps.pedido.adicionarItem).toHaveBeenCalledWith(expect.anything(), 'c-1', expect.objectContaining({ quantidade: 2 }));
    await app.close();
  });

  it('nao permite cancelar item que nao pertence a comanda da mesa', async () => {
    const app = await criarServidor(cenario({ comandaAberta: true }));
    const r = await app.inject({ method: 'DELETE', url: '/demo-mesa-1/comanda/itens/item-de-outra-mesa' });
    expect(r.statusCode).toBe(404);
    await app.close();
  });

  it('confirmacao exige chave de idempotencia', async () => {
    const app = await criarServidor(cenario({ comandaAberta: true }));
    const r = await app.inject({ method: 'POST', url: '/demo-mesa-1/comanda/confirmacao', payload: {} });
    expect(r.statusCode).toBe(400);
    await app.close();
  });

  it('eventos chegam apenas ao stream da comanda certa', () => {
    const streams = new GerenciadorDeStreams(60_000);
    const minha = { write: vi.fn(), end: vi.fn() } as unknown as ServerResponse;
    const outra = { write: vi.fn(), end: vi.fn() } as unknown as ServerResponse;
    streams.registrar('demo:c-1', minha);
    streams.registrar('demo:c-2', outra);
    rotearEventoParaMesa({ chave: 'ticket.status', tipo: 'TicketStatusAlterado', corpo: { tenantId: 'demo', comandaId: 'c-1', status: 'EM_PREPARO' } }, streams);
    expect(String(vi.mocked(minha.write).mock.calls[0][0])).toContain('event: comanda-atualizada');
    expect(outra.write).not.toHaveBeenCalled();
    streams.fechar();
  });
});
