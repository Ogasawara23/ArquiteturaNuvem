import type { Contexto } from '../contexto.js';
import { ClienteHttpResiliente } from './clienteHttpResiliente.js';

export interface TicketDto {
  ticketId: string;
  comandaId: string;
  mesa: string;
  estacaoId: string;
  status: 'RECEBIDO' | 'EM_PREPARO' | 'PRONTO';
  tempoAlvoSegundos: number;
  decorridoSegundos: number;
  situacaoSla: string;
  criadoEm: string;
  itens: Array<{
    itemComandaId: string;
    descricao: string;
    quantidade: number;
    modificadores: string[];
    observacao: string | null;
    cancelado: boolean;
  }>;
}

export class ProducaoClient {
  constructor(private readonly http: ClienteHttpResiliente) {}

  listarTickets(contexto: Contexto, estacaoId: string): Promise<TicketDto[]> {
    return this.http.requisitar({ metodo: 'GET', caminho: `/interno/estacoes/${encodeURIComponent(estacaoId)}/tickets`, contexto });
  }

  avancarStatus(contexto: Contexto, ticketId: string, status: string): Promise<{ ticketId: string; status: string }> {
    return this.http.requisitar({ metodo: 'PATCH', caminho: `/interno/tickets/${encodeURIComponent(ticketId)}/status`, contexto, corpo: { status } });
  }

  expedir(contexto: Contexto, ticketId: string): Promise<void> {
    return this.http.requisitar({ metodo: 'POST', caminho: `/interno/tickets/${encodeURIComponent(ticketId)}/bump`, contexto });
  }
}
