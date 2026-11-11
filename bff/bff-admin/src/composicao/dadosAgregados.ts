import type {
  CatalogoClient,
  ComandaResumo,
  Indicadores,
  ItemResumo,
  PedidoClient,
  ProducaoClient,
  StatusOperacional,
  StatusOperacionalClient,
} from '../clientes/servicos.client.js';
import type { Contexto } from '../contexto.js';

export type SituacaoFonte = 'ok' | 'indisponivel';

export interface Fonte {
  nome: string;
  tipo: 'microsservico' | 'azure-function';
  situacao: SituacaoFonte;
  latenciaMs: number;
  erro?: string;
}

export interface DadosAgregados {
  tenantId: string;
  geradoEm: string;
  fontes: Fonte[];
  catalogo: {
    totalDeItens: number;
    itensDisponiveis: number;
    itensEsgotados: number;
    precoMedioCentavos: number;
    porCategoria: Array<{ categoria: string; itens: number }>;
    itens: ItemResumo[];
  } | null;
  pedidos: {
    comandasAbertas: number;
    comandasFechadasNasUltimas24h: number;
    valorEmAbertoCentavos: number;
    faturamentoNasUltimas24hCentavos: number;
    itensAguardandoCozinha: number;
    comandasRecentes: Array<Pick<ComandaResumo, 'comandaId' | 'mesaRotulo' | 'status' | 'subtotalCentavos' | 'abertaEm'>>;
  } | null;
  statusOperacional: StatusOperacional | null;
  producao: Indicadores | null;
  estimativa: {
    tempoMedioPreparoSegundos: number | null;
    tempoEstimadoComPicoSegundos: number | null;
    explicacao: string;
  };
}

async function medir<T>(nome: string, tipo: Fonte['tipo'], fontes: Fonte[], chamada: () => Promise<T>): Promise<T | null> {
  const inicio = performance.now();
  try {
    const valor = await chamada();
    fontes.push({ nome, tipo, situacao: 'ok', latenciaMs: Math.round(performance.now() - inicio) });
    return valor;
  } catch (erro) {
    fontes.push({ nome, tipo, situacao: 'indisponivel', latenciaMs: Math.round(performance.now() - inicio), erro: (erro as Error).message });
    return null;
  }
}

/**
 * GET /aggregated-data: consome o Microsservico 1 (catalogo-service, MongoDB), o Microsservico 2
 * (pedido-service, Azure SQL) e a Azure Function (status operacional) EM PARALELO e devolve
 * tudo em um unico response. Os indicadores do producao-service entram como enriquecimento.
 * Falha de uma fonte nao derruba a resposta: ela aparece como "indisponivel" em `fontes`.
 */
export class DadosAgregadosComposicao {
  constructor(
    private readonly catalogo: CatalogoClient,
    private readonly pedido: PedidoClient,
    private readonly statusOperacional: StatusOperacionalClient,
    private readonly producao: ProducaoClient,
    private readonly relogio: () => Date = () => new Date(),
  ) {}

  async montar(contexto: Contexto): Promise<DadosAgregados> {
    const fontes: Fonte[] = [];
    const [itens, comandas, status, indicadores] = await Promise.all([
      medir('catalogo-service', 'microsservico', fontes, () => this.catalogo.listarItens(contexto)),
      medir('pedido-service', 'microsservico', fontes, () => this.pedido.listarComandas(contexto)),
      medir('azure-function:status-operacional', 'azure-function', fontes, () => this.statusOperacional.consultar(contexto)),
      medir('producao-service', 'microsservico', fontes, () => this.producao.indicadores(contexto, 24)),
    ]);
    const agora = this.relogio();
    return {
      tenantId: contexto.tenantId,
      geradoEm: agora.toISOString(),
      fontes,
      catalogo: itens ? resumirCatalogo(itens) : null,
      pedidos: comandas ? resumirPedidos(comandas, agora) : null,
      statusOperacional: status,
      producao: indicadores,
      estimativa: estimar(indicadores, status),
    };
  }
}

export function resumirCatalogo(itens: ItemResumo[]): NonNullable<DadosAgregados['catalogo']> {
  const porCategoria = new Map<string, number>();
  for (const item of itens) porCategoria.set(item.categoria, (porCategoria.get(item.categoria) ?? 0) + 1);
  const disponiveis = itens.filter((i) => i.disponivel).length;
  return {
    totalDeItens: itens.length,
    itensDisponiveis: disponiveis,
    itensEsgotados: itens.length - disponiveis,
    precoMedioCentavos: itens.length ? Math.round(itens.reduce((s, i) => s + i.precoCentavos, 0) / itens.length) : 0,
    porCategoria: [...porCategoria].map(([categoria, n]) => ({ categoria, itens: n })),
    itens,
  };
}

export function resumirPedidos(comandas: ComandaResumo[], agora: Date): NonNullable<DadosAgregados['pedidos']> {
  const limite = agora.getTime() - 24 * 3600 * 1000;
  const abertas = comandas.filter((c) => c.status === 'ABERTA');
  const fechadasRecentes = comandas.filter((c) => c.status === 'FECHADA' && c.fechadaEm && Date.parse(c.fechadaEm) >= limite);
  return {
    comandasAbertas: abertas.length,
    comandasFechadasNasUltimas24h: fechadasRecentes.length,
    valorEmAbertoCentavos: abertas.reduce((s, c) => s + c.subtotalCentavos, 0),
    faturamentoNasUltimas24hCentavos: fechadasRecentes.reduce((s, c) => s + (c.totalCentavos ?? c.subtotalCentavos), 0),
    itensAguardandoCozinha: abertas.flatMap((c) => c.itens)
      .filter((i) => i.status === 'CONFIRMADO' || i.status === 'EM_PRODUCAO')
      .reduce((s, i) => s + i.quantidade, 0),
    comandasRecentes: comandas.slice(0, 10).map(({ comandaId, mesaRotulo, status, subtotalCentavos, abertaEm }) =>
      ({ comandaId, mesaRotulo, status, subtotalCentavos, abertaEm })),
  };
}

export function estimar(indicadores: Indicadores | null, status: StatusOperacional | null): DadosAgregados['estimativa'] {
  const comPreparo = indicadores?.estacoes.filter((e) => e.tempoMedioPreparoSegundos > 0) ?? [];
  if (!comPreparo.length) {
    return { tempoMedioPreparoSegundos: null, tempoEstimadoComPicoSegundos: null, explicacao: 'sem tickets concluidos nas ultimas 24 h' };
  }
  const media = Math.round(comPreparo.reduce((s, e) => s + e.tempoMedioPreparoSegundos, 0) / comPreparo.length);
  const fator = status?.multiplicadorTempoPreparo ?? 1;
  return {
    tempoMedioPreparoSegundos: media,
    tempoEstimadoComPicoSegundos: Math.round(media * fator),
    explicacao: status
      ? `media das estacoes (producao-service) x ${fator} (Azure Function: ${status.faixaHoraria})`
      : 'media das estacoes; Azure Function indisponivel, sem ajuste de pico',
  };
}
