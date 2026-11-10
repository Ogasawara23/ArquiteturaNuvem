/**
 * Contrato de sessao injetado pelo shell (por props). O remote nunca le token do
 * localStorage/sessionStorage: a regra e verificada pelo teste de arquitetura.
 */
export interface Sessao {
  apiUrl: string;
  token?: string;
  tenantId: string;
  usuario: string;
  papeis: string[];
  /** URL publica do shell-mesa, usada para montar o QR Code de cada mesa. */
  urlMesa?: string;
}
