/**
 * Cliente OIDC minimo (Authorization Code + PKCE) contra o Keycloak. O token de acesso fica
 * APENAS em memoria (Secao 5.1). Somente o verificador PKCE e o state transitam pelo
 * sessionStorage durante o redirecionamento, e sao apagados logo em seguida.
 */
export interface SessaoAutenticada {
  token: string;
  tenantId: string;
  usuario: string;
  papeis: string[];
  expiraEm: number;
}

export interface ConfiguracaoOidc {
  emissor: string; // ex.: https://auth.exemplo.app/realms/smartkds
  clienteId: string;
  redirecionamento: string;
}

const CHAVE = 'smartkds.pkce';

function base64Url(bytes: ArrayBuffer | Uint8Array): string {
  const binario = String.fromCharCode(...new Uint8Array(bytes));
  return btoa(binario).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function lerClaims(token: string): Record<string, unknown> {
  const carga = token.split('.')[1] ?? '';
  const json = atob(carga.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(carga.length / 4) * 4, '='));
  return JSON.parse(decodeURIComponent(escape(json)));
}

export function sessaoDoToken(token: string): SessaoAutenticada {
  const c = lerClaims(token) as { tenant_id?: string; preferred_username?: string; sub?: string; exp?: number; realm_access?: { roles?: string[] } };
  return {
    token,
    tenantId: c.tenant_id ?? '',
    usuario: c.preferred_username ?? c.sub ?? 'usuario',
    papeis: (c.realm_access?.roles ?? []).map((r) => r.toUpperCase()),
    expiraEm: (c.exp ?? 0) * 1000,
  };
}

export async function iniciarLogin(cfg: ConfiguracaoOidc): Promise<void> {
  const verificador = base64Url(crypto.getRandomValues(new Uint8Array(48)));
  const desafio = base64Url(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verificador)));
  const state = base64Url(crypto.getRandomValues(new Uint8Array(16)));
  sessionStorage.setItem(CHAVE, JSON.stringify({ verificador, state }));
  const url = new URL(`${cfg.emissor}/protocol/openid-connect/auth`);
  url.search = new URLSearchParams({
    response_type: 'code', client_id: cfg.clienteId, redirect_uri: cfg.redirecionamento, scope: 'openid',
    code_challenge: desafio, code_challenge_method: 'S256', state,
  }).toString();
  window.location.assign(url.toString());
}

/** Conclui o login se a URL contem ?code=...&state=... vindos do Keycloak. */
export async function concluirLogin(cfg: ConfiguracaoOidc): Promise<SessaoAutenticada | null> {
  const parametros = new URLSearchParams(window.location.search);
  const code = parametros.get('code');
  const pendente = sessionStorage.getItem(CHAVE);
  if (!code || !pendente) return null;
  sessionStorage.removeItem(CHAVE);
  const { verificador, state } = JSON.parse(pendente) as { verificador: string; state: string };
  if (parametros.get('state') !== state) throw new Error('state OIDC invalido');
  const resposta = await fetch(`${cfg.emissor}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'authorization_code', code, client_id: cfg.clienteId,
      redirect_uri: cfg.redirecionamento, code_verifier: verificador,
    }),
  });
  if (!resposta.ok) throw new Error('falha ao obter o token no Keycloak');
  const { access_token } = (await resposta.json()) as { access_token: string };
  window.history.replaceState({}, '', cfg.redirecionamento + window.location.hash);
  return sessaoDoToken(access_token);
}

export function sair(cfg: ConfiguracaoOidc): void {
  const url = new URL(`${cfg.emissor}/protocol/openid-connect/logout`);
  url.search = new URLSearchParams({ client_id: cfg.clienteId, post_logout_redirect_uri: cfg.redirecionamento }).toString();
  window.location.assign(url.toString());
}
