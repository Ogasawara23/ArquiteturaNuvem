import type { CacheEmMemoria } from '../cache/cacheEmMemoria.js';
import type { EstabelecimentoClient, EstacaoDto } from '../clientes/estabelecimento.client.js';
import type { ProducaoClient, TicketDto } from '../clientes/producao.client.js';
import type { Contexto } from '../contexto.js';

export type Destaque = 'normal' | 'atencao' | 'atrasado';

export interface TicketViewModel {
  ticketId: string;
  mesa: string;
  status: TicketDto['status'];
  itens: Array<{ descricao: string; quantidade: number; modificadores: string[]; observacao: string | null; cancelado: boolean }>;
  criadoEm: string;
  decorridoSegundos: number;
  tempoAlvoSegundos: number;
  destaque: Destaque;
}

export interface FilaDaEstacao {
  estacao: { codigo: string; nome: string; percentualAtencao: number };
  geradoEm: string;
  tickets: TicketViewModel[];
}

const PERCENTUAL_ATENCAO_PADRAO = 70;

/** Regra de apresentacao (RF15), calculada no servidor para que todas as telas concordem. */
export function calcularDestaque(decorridoSegundos: number, tempoAlvoSegundos: number, percentualAtencao: number): Destaque {
  if (decorridoSegundos > tempoAlvoSegundos) return 'atrasado';
  if (decorridoSegundos * 100 > tempoAlvoSegundos * percentualAtencao) return 'atencao';
  return 'normal';
}

/**
 * Componente "composicao" da Figura 5: une ticket (producao-service) e SLA da estacao
 * (estabelecimento-service, em cache de 60 s). Se a configuracao estiver indisponivel,
 * usa o percentual padrao: a tela da cozinha nunca para por causa da configuracao.
 */
export class TicketsDaEstacaoViewModelBuilder {
  constructor(
    private readonly producao: ProducaoClient,
    private readonly estabelecimento: EstabelecimentoClient,
    private readonly cacheDeEstacoes: CacheEmMemoria<EstacaoDto>,
  ) {}

  async montar(contexto: Contexto, estacaoId: string): Promise<FilaDaEstacao> {
    const [tickets, estacao] = await Promise.all([
      this.producao.listarTickets(contexto, estacaoId),
      this.cacheDeEstacoes
        .obter(`${contexto.tenantId}:${estacaoId}`, () => this.estabelecimento.obterEstacao(contexto, estacaoId))
        .catch(() => undefined),
    ]);
    const percentual = estacao?.percentualAtencao ?? PERCENTUAL_ATENCAO_PADRAO;
    return {
      estacao: { codigo: estacaoId, nome: estacao?.nome ?? estacaoId, percentualAtencao: percentual },
      geradoEm: new Date().toISOString(),
      tickets: tickets.map((t) => ({
        ticketId: t.ticketId,
        mesa: t.mesa,
        status: t.status,
        itens: t.itens.map(({ descricao, quantidade, modificadores, observacao, cancelado }) => ({
          descricao, quantidade, modificadores, observacao, cancelado,
        })),
        criadoEm: t.criadoEm,
        decorridoSegundos: t.decorridoSegundos,
        tempoAlvoSegundos: t.tempoAlvoSegundos,
        destaque: calcularDestaque(t.decorridoSegundos, t.tempoAlvoSegundos, percentual),
      })),
    };
  }
}
