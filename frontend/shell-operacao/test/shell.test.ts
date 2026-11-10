import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it, vi } from 'vitest';

vi.mock('kds/TelaProducao', () => ({ default: () => null }), { virtual: true } as never);
vi.mock('cardapio/TelaCardapio', () => ({ default: () => null }), { virtual: true } as never);
vi.mock('gestao/TelaGestao', () => ({ default: () => null }), { virtual: true } as never);

import { sessaoDoToken } from '../src/sessao/oidcClient';

const base64Url = (o: unknown) => Buffer.from(JSON.stringify(o)).toString('base64url');

describe('sessao a partir do JWT do Keycloak', () => {
  it('extrai tenant_id, usuario e papeis do realm', () => {
    const token = `${base64Url({ alg: 'RS256' })}.${base64Url({ tenant_id: 'rest-trattoria-centro', preferred_username: 'ana', exp: 2_000_000_000, realm_access: { roles: ['gerente', 'offline_access'] } })}.assinatura`;
    const s = sessaoDoToken(token);
    expect(s).toMatchObject({ tenantId: 'rest-trattoria-centro', usuario: 'ana', papeis: ['GERENTE', 'OFFLINE_ACCESS'] });
  });
});

describe('remotes por papel', () => {
  it('cozinha ve apenas o KDS', async () => {
    const { remotesPermitidos } = await import('../src/remotes/remotes.config');
    expect(remotesPermitidos(['COZINHA']).map((r) => r.id)).toEqual(['kds']);
    expect(remotesPermitidos(['GERENTE']).map((r) => r.id)).toEqual(['kds', 'cardapio', 'gestao']);
  });
});

describe('arquitetura do shell', () => {
  it('o shell nao importa codigo interno dos remotes, apenas os modulos expostos', () => {
    const raiz = join(__dirname, '..', 'src');
    const arquivos = (p: string): string[] => readdirSync(p, { withFileTypes: true })
      .flatMap((e) => (e.isDirectory() ? arquivos(join(p, e.name)) : /\.tsx?$/.test(e.name) ? [join(p, e.name)] : []));
    const proibidos = arquivos(raiz).filter((a) => /from ['"]\.\.?\/.*mfe-|['"](kds|cardapio|gestao)\/(?!TelaProducao|TelaCardapio|TelaGestao)/.test(readFileSync(a, 'utf-8')));
    expect(proibidos).toEqual([]);
  });
});
