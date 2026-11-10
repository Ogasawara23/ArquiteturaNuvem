import { readdirSync, readFileSync } from 'node:fs';
import { join, relative } from 'node:path';
import { describe, expect, it } from 'vitest';

/**
 * Testes de arquitetura do BFF (equivalente ao dependency-cruiser da Secao 8.8.5):
 * a direcao das dependencias entre os componentes da Figura 5 e verificada a cada build.
 */
const RAIZ = join(__dirname, '..', 'src');

function arquivos(pasta = RAIZ): string[] {
  return readdirSync(pasta, { withFileTypes: true }).flatMap((e) =>
    e.isDirectory() ? arquivos(join(pasta, e.name)) : e.name.endsWith('.ts') ? [join(pasta, e.name)] : []);
}

function importsDe(arquivo: string): string[] {
  const codigo = readFileSync(arquivo, 'utf-8');
  return [...codigo.matchAll(/^\s*import\s[^'"]*['"]([^'"]+)['"]/gm)].map((m) => m[1]);
}

function componente(arquivo: string): string {
  const rel = relative(RAIZ, arquivo).replace(/\\/g, '/');
  return rel.includes('/') ? rel.split('/')[0] : 'raiz';
}

function violacoes(de: string, proibidos: RegExp[]): string[] {
  return arquivos()
    .filter((a) => componente(a) === de)
    .flatMap((a) => importsDe(a).filter((i) => proibidos.some((p) => p.test(i))).map((i) => `${relative(RAIZ, a)} -> ${i}`));
}

describe('arquitetura do bff', () => {
  it('composicao nao conhece HTTP de entrada (fastify) nem rotas', () => {
    expect(violacoes('composicao', [/^fastify$/, /\/rotas\//])).toEqual([]);
  });

  it('clientes nao dependem de composicao, rotas nem sse', () => {
    expect(violacoes('clientes', [/\/composicao\//, /\/rotas\//, /\/sse\//])).toEqual([]);
  });

  it('sse e eventos nao chamam microsservicos', () => {
    expect([...violacoes('sse', [/\/clientes\//]), ...violacoes('eventos', [/\/clientes\//, /\/rotas\//])]).toEqual([]);
  });

  it('o BFF nao possui banco de dados nem regra de negocio de outro contexto (ADR-03)', () => {
    const proibidos = [/^pg$/, /^mongodb$/, /^mongoose$/, /^@prisma\//, /^typeorm$/, /^sequelize$/, /^redis$/];
    const achados = arquivos().flatMap((a) => importsDe(a).filter((i) => proibidos.some((p) => p.test(i))));
    expect(achados).toEqual([]);
  });

  it('nenhum BFF importa codigo de outro BFF', () => {
    const achados = arquivos().flatMap((a) => importsDe(a).filter((i) => /bff-(mesa|kds|admin)/.test(i)));
    expect(achados).toEqual([]);
  });
});
