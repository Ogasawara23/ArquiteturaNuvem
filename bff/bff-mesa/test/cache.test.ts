import { describe, expect, it } from 'vitest';
import { CacheEmMemoria } from '../src/cache/cacheEmMemoria.js';
import { ErroHttp } from '../src/contexto.js';

describe('CacheEmMemoria', () => {
  it('serve o valor expirado quando a dependencia esta fora do ar (5xx)', async () => {
    let agora = 0;
    const cache = new CacheEmMemoria<string>(1000, () => agora);
    await cache.obter('k', async () => 'mesa-1');
    agora = 5000;
    await expect(cache.obter('k', async () => { throw new ErroHttp(503, 'Indisponivel', 'fora'); })).resolves.toBe('mesa-1');
  });

  it('QR Code rotacionado: 404 da dependencia invalida o valor guardado em vez de servi-lo', async () => {
    let agora = 0;
    const cache = new CacheEmMemoria<string>(1000, () => agora);
    await cache.obter('token-antigo', async () => 'mesa-1');
    agora = 5000;
    const nao = async (): Promise<string> => { throw new ErroHttp(404, 'Nao encontrado', 'token invalido'); };
    await expect(cache.obter('token-antigo', nao)).rejects.toMatchObject({ status: 404 });
    await expect(cache.obter('token-antigo', nao)).rejects.toMatchObject({ status: 404 });
  });
});
