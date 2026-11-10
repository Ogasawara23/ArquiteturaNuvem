/**
 * Extrai o qrToken da URL codificada no QR Code. Aceita /m/{token} (padrao), #/m/{token}
 * (hospedagem estatica sem rewrite) e ?qr={token}. O token e opaco e validado no bff-mesa.
 */
export function qrTokenDaUrl(local: Pick<Location, 'pathname' | 'hash' | 'search'>): string | null {
  const caminho = /\/m\/([A-Za-z0-9_-]{6,120})\/?$/.exec(local.pathname)?.[1];
  const hash = /^#\/?m\/([A-Za-z0-9_-]{6,120})/.exec(local.hash)?.[1];
  const consulta = new URLSearchParams(local.search).get('qr');
  const token = caminho ?? hash ?? consulta;
  return token && /^[A-Za-z0-9_-]{6,120}$/.test(token) ? token : null;
}
