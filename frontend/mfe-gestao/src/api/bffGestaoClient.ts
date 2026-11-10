import type { Sessao } from './sessao';

export interface Fonte { nome: string; tipo: string; situacao: 'ok' | 'indisponivel'; latenciaMs: number; erro?: string }
export interface DadosAgregados {
  tenantId: string;
  geradoEm: string;
  fontes: Fonte[];
  catalogo: { totalDeItens: number; itensDisponiveis: number; itensEsgotados: number; precoMedioCentavos: number; porCategoria: Array<{ categoria: string; itens: number }> } | null;
  pedidos: { comandasAbertas: number; comandasFechadasNasUltimas24h: number; valorEmAbertoCentavos: number; faturamentoNasUltimas24hCentavos: number; itensAguardandoCozinha: number } | null;
  statusOperacional: { faixaHoraria: string; emHorarioDePico: boolean; multiplicadorTempoPreparo: number; mensagem: string; horaLocal: string; diaDaSemana: string } | null;
  producao: { estacoes: Array<{ estacaoId: string; ticketsCriados: number; ticketsExpedidos: number; ticketsAtivos: number; ticketsAtrasados: number; taxaDeAtraso: number; tempoMedioPreparoSegundos: number }>; volumePorHora: Array<{ hora: number; tickets: number }> } | null;
  estimativa: { tempoMedioPreparoSegundos: number | null; tempoEstimadoComPicoSegundos: number | null; explicacao: string };
}
export interface Comanda {
  comandaId: string; mesaRotulo: string; status: string; subtotalCentavos: number; totalCentavos: number | null;
  abertaEm: string; itens: Array<{ descricao: string; quantidade: number; status: string; totalCentavos: number }>;
}
export interface Mesa { mesaId: string; rotulo: string; qrToken: string; ativa: boolean }
export interface Estacao { codigo: string; nome: string; ordemExibicao: number; percentualAtencao: number }
export interface Roteamento { estacaoPadrao: string | null; regras: Array<{ itemCardapioId: string | null; categoria: string | null; estacaoId: string; prioridade: number }> }
export interface RegistroAuditoria { id: string; usuarioId: string; acao: string; comandaId: string; detalhe: Record<string, unknown>; ocorridoEm: string }

/** Cliente do bff-admin: unico ponto do remote que fala HTTP. */
export function criarClienteGestao(sessao: Sessao) {
  const base = `${sessao.apiUrl.replace(/\/$/, '')}/bff/admin`;
  async function chamar<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
    const h: Record<string, string> = { 'content-type': 'application/json' };
    if (sessao.token) h.authorization = `Bearer ${sessao.token}`;
    else h['x-tenant-id'] = sessao.tenantId;
    const r = await fetch(`${base}${caminho}`, { method: metodo, headers: h, body: corpo === undefined ? undefined : JSON.stringify(corpo) });
    if (!r.ok) {
      const p = await r.json().catch(() => ({}));
      throw new Error(p.detail ?? `erro ${r.status}`);
    }
    return (r.status === 204 ? undefined : await r.json()) as T;
  }
  return {
    agregado: () => chamar<DadosAgregados>('GET', '/aggregated-data'),
    comandas: (status?: string) => chamar<Comanda[]>('GET', `/comandas${status ? `?status=${status}` : ''}`),
    cancelarComanda: (id: string, motivo: string) => chamar<void>('DELETE', `/comandas/${id}?motivo=${encodeURIComponent(motivo)}`),
    fechar: (id: string, percentualTaxaServico: number) => chamar<{ totalCentavos: number }>('POST', `/comandas/${id}/fechamento`, { percentualTaxaServico }),
    mesas: () => chamar<Mesa[]>('GET', '/mesas'),
    criarMesa: (rotulo: string) => chamar<Mesa>('POST', '/mesas', { rotulo }),
    rotacionar: (mesaId: string) => chamar<Mesa>('POST', `/mesas/${mesaId}/qr-token`),
    estacoes: () => chamar<Estacao[]>('GET', '/estacoes'),
    criarEstacao: (nome: string, codigo: string) => chamar<Estacao>('POST', '/estacoes', { nome, codigo: codigo || undefined }),
    roteamento: () => chamar<Roteamento>('GET', '/roteamento'),
    salvarRoteamento: (r: Roteamento) => chamar<void>('PUT', '/roteamento', r),
    auditoria: () => chamar<RegistroAuditoria[]>('GET', '/auditoria'),
  };
}

export type ClienteGestao = ReturnType<typeof criarClienteGestao>;
