import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { Ticket } from '../src/api/bffKdsClient';
import { CartaoDeTicket } from '../src/componentes/CartaoDeTicket';
import { formatarDuracao } from '../src/componentes/Cronometro';

afterEach(cleanup);

const ticket = (sobrescrever: Partial<Ticket> = {}): Ticket => ({
  ticketId: 't1', mesa: 'Mesa 12', status: 'RECEBIDO', criadoEm: '', decorridoSegundos: 750, tempoAlvoSegundos: 720,
  destaque: 'atrasado', itens: [{ descricao: 'Hamburguer', quantidade: 2, modificadores: ['Ao ponto'], observacao: 'sem cebola', cancelado: false }],
  ...sobrescrever,
});

describe('CartaoDeTicket', () => {
  it('exibe o destaque correto por faixa de atraso', () => {
    render(<CartaoDeTicket ticket={ticket()} recebidoEm={Date.now()} acoes={{ iniciar: vi.fn(), pronto: vi.fn(), bump: vi.fn() }} ocupado={false} />);
    expect(screen.getByTestId('ticket').dataset.destaque).toBe('atrasado');
    expect(screen.getByText('Atrasado')).toBeTruthy();
    expect(screen.getByText(/sem cebola/)).toBeTruthy();
  });

  it('mostra uma unica acao por estado (um gesto por transicao)', () => {
    const acoes = { iniciar: vi.fn(), pronto: vi.fn(), bump: vi.fn() };
    render(<CartaoDeTicket ticket={ticket({ status: 'PRONTO', destaque: 'normal' })} recebidoEm={Date.now()} acoes={acoes} ocupado={false} />);
    expect(screen.queryByText('Iniciar preparo')).toBeNull();
    fireEvent.click(screen.getByText('Bump (expedir)'));
    expect(acoes.bump).toHaveBeenCalledWith('t1');
  });

  it('formata o cronometro em mm:ss', () => {
    expect(formatarDuracao(754)).toBe('12:34');
  });
});

describe('arquitetura do microfrontend (Secao 8.8.5)', () => {
  const raiz = join(__dirname, '..', 'src');
  const arquivos = (p: string): string[] => readdirSync(p, { withFileTypes: true })
    .flatMap((e) => (e.isDirectory() ? arquivos(join(p, e.name)) : /\.tsx?$/.test(e.name) ? [join(p, e.name)] : []));
  const codigo = arquivos(raiz).map((a) => ({ a, c: readFileSync(a, 'utf-8') }));

  it('remotes nao se importam: a composicao acontece no shell', () => {
    expect(codigo.filter(({ c }) => /from ['"](comanda|cardapio|gestao|kds)\//.test(c)).map(({ a }) => a)).toEqual([]);
  });

  it('remote nao le token do storage: a sessao vem do shell', () => {
    expect(codigo.filter(({ c }) => /\b(localStorage|sessionStorage)\s*\./.test(c)).map(({ a }) => a)).toEqual([]);
  });

  it('componentes nao chamam fetch direto: toda chamada passa por src/api', () => {
    expect(codigo.filter(({ a, c }) => !a.includes(`${join('src', 'api')}`) && /\bfetch\(/.test(c)).map(({ a }) => a)).toEqual([]);
  });
});
