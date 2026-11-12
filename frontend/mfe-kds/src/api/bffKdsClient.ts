import type { Sessao } from './sessao';

export type Destaque = 'normal' | 'atencao' | 'atrasado';
export type StatusTicket = 'RECEBIDO' | 'EM_PREPARO' | 'PRONTO';

export interface Ticket {
  ticketId: string;
  mesa: string;
  status: StatusTicket;
  itens: Array<{ descricao: string; quantidade: number; modificadores: string[]; observacao: string | null; cancelado: boolean }>;
  criadoEm: string;
  decorridoSegundos: number;
  tempoAlvoSegundos: number;
  destaque: Destaque;
}

export interface FilaDaEstacao {
  estacao: { codigo: string; nome: string; percentualAtencao: number };
  geradoEm: string;
  tickets: Ticket[];
}

export interface Estacao {
  codigo: string;
  nome: string;
}

/** Unico ponto do remote que fala HTTP: todas as chamadas vao ao API Gateway em /bff/kds/**. */
export function criarClienteKds(sessao: Sessao) {
  const base = `${sessao.apiUrl.replace(/\/$/, '')}/bff/kds`;
  const cabecalhos = (comCorpo: boolean): Record<string, string> => {
    const h: Record<string, string> = comCorpo ? { 'content-type': 'application/json' } : {};
    if (sessao.token) h.authorization = `Bearer ${sessao.token}`;
    else h['x-tenant-id'] = sessao.tenantId; // modo demonstracao (gateway sem Keycloak)
    return h;
  };
  async function chamar<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
    const r = await fetch(`${base}${caminho}`, { method: metodo, headers: cabecalhos(corpo !== undefined), body: corpo ? JSON.stringify(corpo) : undefined });
    if (!r.ok) {
      const problema = await r.json().catch(() => ({}));
      throw new Error(problema.detail ?? `erro ${r.status}`);
    }
    return (r.status === 204 ? undefined : await r.json()) as T;
  }
  return {
    estacoes: () => chamar<Estacao[]>('GET', '/estacoes'),
    fila: (estacao: string) => chamar<FilaDaEstacao>('GET', `/estacoes/${encodeURIComponent(estacao)}/tickets`),
    avancar: (ticketId: string, status: 'EM_PREPARO' | 'PRONTO') => chamar<void>('PATCH', `/tickets/${ticketId}/status`, { status }),
    bump: (ticketId: string) => chamar<void>('POST', `/tickets/${ticketId}/bump`),
    urlDoStream: (estacao: string) =>
      `${base}/estacoes/${encodeURIComponent(estacao)}/stream${sessao.token ? `?access_token=${encodeURIComponent(sessao.token)}` : ''}`,
  };
}
