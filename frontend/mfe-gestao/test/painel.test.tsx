import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ClienteGestao, DadosAgregados } from '../src/api/bffGestaoClient';
import { escreverRegras, lerRegras } from '../src/componentes/formatos';
import { Painel } from '../src/componentes/Painel';

afterEach(cleanup);

const dados: DadosAgregados = {
  tenantId: 'demo', geradoEm: new Date().toISOString(),
  fontes: [
    { nome: 'catalogo-service', tipo: 'microsservico', situacao: 'ok', latenciaMs: 12 },
    { nome: 'azure-function:status-operacional', tipo: 'azure-function', situacao: 'indisponivel', latenciaMs: 3000, erro: 'timeout' },
  ],
  catalogo: { totalDeItens: 10, itensDisponiveis: 9, itensEsgotados: 1, precoMedioCentavos: 3000, porCategoria: [{ categoria: 'Lanches', itens: 4 }] },
  pedidos: { comandasAbertas: 3, comandasFechadasNasUltimas24h: 5, valorEmAbertoCentavos: 15000, faturamentoNasUltimas24hCentavos: 99000, itensAguardandoCozinha: 4 },
  statusOperacional: null,
  producao: { estacoes: [], volumePorHora: [] },
  estimativa: { tempoMedioPreparoSegundos: null, tempoEstimadoComPicoSegundos: null, explicacao: 'sem tickets' },
};

describe('Painel', () => {
  it('mostra KPIs do endpoint agregado e a situacao de cada fonte', async () => {
    const api = { agregado: vi.fn(async () => dados) } as unknown as ClienteGestao;
    render(<Painel api={api} />);
    expect(await screen.findByText('Comandas abertas')).toBeTruthy();
    expect(screen.getByText('3')).toBeTruthy();
    expect(screen.getByText('indisponivel')).toBeTruthy();
    expect(screen.getByText('timeout')).toBeTruthy();
  });
});

describe('regras de roteamento em texto', () => {
  it('ida e volta', () => {
    const texto = 'padrao -> cozinha\ncategoria:Bebidas -> bar\nitem:abc-123 -> grill (5)';
    const r = lerRegras(texto);
    expect(r.estacaoPadrao).toBe('cozinha');
    expect(r.regras[1]).toEqual({ categoria: null, itemCardapioId: 'abc-123', estacaoId: 'grill', prioridade: 5 });
    expect(escreverRegras(r)).toBe(texto);
  });
});
