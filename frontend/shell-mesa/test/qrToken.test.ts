import { describe, expect, it } from 'vitest';
import { qrTokenDaUrl } from '../src/qrToken';

const local = (pathname: string, hash = '', search = '') => ({ pathname, hash, search });

describe('qrToken da URL do QR Code', () => {
  it('aceita /m/{token}, #/m/{token} e ?qr=', () => {
    expect(qrTokenDaUrl(local('/m/demo-mesa-1'))).toBe('demo-mesa-1');
    expect(qrTokenDaUrl(local('/', '#/m/Ab_c-123456'))).toBe('Ab_c-123456');
    expect(qrTokenDaUrl(local('/', '', '?qr=demo-mesa-2'))).toBe('demo-mesa-2');
  });

  it('recusa tokens malformados antes de qualquer chamada', () => {
    expect(qrTokenDaUrl(local('/m/<script>'))).toBeNull();
    expect(qrTokenDaUrl(local('/', '', '?qr=abc'))).toBeNull();
    expect(qrTokenDaUrl(local('/'))).toBeNull();
  });
});
