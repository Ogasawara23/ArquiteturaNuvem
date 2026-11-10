import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

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
