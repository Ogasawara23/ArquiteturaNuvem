import type { Contexto } from '../contexto.js';
import { ErroHttp } from '../contexto.js';
import type { ClienteHttpResiliente } from './clienteHttpResiliente.js';

export interface ItemResumo {
  id: string;
  nome: string;
  categoria: string;
  precoCentavos: number;
  estacaoSugerida: string;
  tempoAlvoPreparoSegundos: number;
  disponivel: boolean;
}

export interface ComandaResumo {
  comandaId: string;
  mesaRotulo: string;
  status: string;
  subtotalCentavos: number;
  totalCentavos: number | null;
  abertaEm: string;
  fechadaEm: string | null;
  itens: Array<{ status: string; quantidade: number }>;
}

export interface Indicadores {
  desde: string;
  estacoes: Array<{ estacaoId: string; ticketsCriados: number; ticketsExpedidos: number; ticketsAtivos: number; ticketsAtrasados: number; taxaDeAtraso: number; tempoMedioPreparoSegundos: number }>;
  volumePorHora: Array<{ hora: number; tickets: number }>;
}

export interface StatusOperacional {
  faixaHoraria: string;
  emHorarioDePico: boolean;
  multiplicadorTempoPreparo: number;
  mensagem: string;
  horaLocal: string;
  diaDaSemana: string;
}

/**
 * Repasse generico (proxy) para as rotas CRUD administrativas: o BFF nao reinterpreta
 * o contrato do servico, apenas autentica o contexto e encaminha (ADR-03).
 */
export class Repassador {
  constructor(readonly http: ClienteHttpResiliente) {}

  repassar<T = unknown>(contexto: Contexto, metodo: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE', caminho: string,
                        corpo?: unknown, consulta?: Record<string, string | number | undefined>): Promise<T> {
    return this.http.requisitar<T>({ metodo, caminho, contexto, corpo, consulta });
  }
}

/** Microsservico 1 (MongoDB). */
export class CatalogoClient extends Repassador {
  listarItens(contexto: Contexto): Promise<ItemResumo[]> {
    return this.repassar(contexto, 'GET', '/interno/itens');
  }
}

/** Microsservico 2 (Azure SQL Database). */
export class PedidoClient extends Repassador {
  listarComandas(contexto: Contexto, status?: string): Promise<ComandaResumo[]> {
    return this.repassar(contexto, 'GET', '/interno/comandas', undefined, { status });
  }
}

export class ProducaoClient extends Repassador {
  indicadores(contexto: Contexto, horas = 24): Promise<Indicadores> {
    return this.repassar(contexto, 'GET', '/interno/indicadores', undefined, { horas });
  }
}

export class EstabelecimentoClient extends Repassador {}

/** Azure Function (HTTP Trigger) de status operacional. */
export class StatusOperacionalClient {
  constructor(private readonly url: string, private readonly chave: string, private readonly timeoutMs = 3000) {}

  async consultar(contexto: Contexto, fuso = 'America/Sao_Paulo'): Promise<StatusOperacional> {
    const url = new URL(this.url);
    url.searchParams.set('tenantId', contexto.tenantId);
    url.searchParams.set('fuso', fuso);
    const cabecalhos: Record<string, string> = { 'x-correlation-id': contexto.correlationId };
    if (this.chave) cabecalhos['x-functions-key'] = this.chave;
    let resposta: Response;
    try {
      resposta = await fetch(url, { headers: cabecalhos, signal: AbortSignal.timeout(this.timeoutMs) });
    } catch {
      throw new ErroHttp(503, 'Dependencia indisponivel', 'Azure Function indisponivel');
    }
    if (!resposta.ok) throw new ErroHttp(503, 'Dependencia indisponivel', `Azure Function respondeu ${resposta.status}`);
    return (await resposta.json()) as StatusOperacional;
  }
}
