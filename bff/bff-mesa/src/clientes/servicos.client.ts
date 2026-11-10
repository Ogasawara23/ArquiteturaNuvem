import type { Contexto } from '../contexto.js';
import type { ClienteHttpResiliente } from './clienteHttpResiliente.js';

export interface MesaResolvida {
  tenantId: string;
  mesaId: string;
  rotulo: string;
  nomeEstabelecimento: string;
  percentualTaxaServico: number;
}

export interface Opcao { nome: string; precoAdicionalCentavos: number; disponivel: boolean }
export interface Grupo { nome: string; obrigatorio: boolean; minimoSelecoes: number; maximoSelecoes: number; opcoes: Opcao[] }
export interface ItemDoCardapio {
  itemCardapioId: string;
  nome: string;
  descricao: string | null;
  categoria: string;
  precoCentavos: number;
  estacaoSugerida: string;
  tempoAlvoPreparoSegundos: number;
  imagemUrl: string | null;
  gruposModificadores: Grupo[];
}
export interface CardapioAtivo { versaoId: string; publicadaEm: string; categorias: Array<{ nome: string; itens: ItemDoCardapio[] }> }

export interface ItemDaComanda {
  itemId: string;
  descricao: string;
  quantidade: number;
  precoUnitarioCentavos: number;
  totalCentavos: number;
  modificadores: string[];
  observacao: string | null;
  status: string;
  podeSerCancelado: boolean;
}
export interface Comanda {
  comandaId: string;
  mesaId: string;
  mesaRotulo: string;
  status: string;
  subtotalCentavos: number;
  itens: ItemDaComanda[];
}

/** estabelecimento-service: resolucao do QR Code (rota interna sem tenant). */
export class EstabelecimentoClient {
  constructor(private readonly http: ClienteHttpResiliente) {}

  resolverQrToken(qrToken: string, correlationId: string): Promise<MesaResolvida> {
    return this.http.requisitar({
      metodo: 'GET',
      caminho: `/interno/mesas/por-token/${encodeURIComponent(qrToken)}`,
      contexto: { tenantId: '', usuarioId: 'bff-mesa', papeis: [], correlationId },
    });
  }
}

/** catalogo-service (Microsservico 1): cardapio ativo do tenant. */
export class CatalogoClient {
  constructor(private readonly http: ClienteHttpResiliente) {}

  cardapioAtivo(contexto: Contexto): Promise<CardapioAtivo> {
    return this.http.requisitar({ metodo: 'GET', caminho: '/interno/cardapios/ativo', contexto });
  }
}

/** pedido-service (Microsservico 2): comanda da mesa. */
export class PedidoClient {
  constructor(private readonly http: ClienteHttpResiliente) {}

  comandasAbertasDaMesa(contexto: Contexto, mesaId: string): Promise<Comanda[]> {
    return this.http.requisitar({ metodo: 'GET', caminho: '/interno/comandas', contexto, consulta: { mesaId, status: 'ABERTA' } });
  }

  obter(contexto: Contexto, comandaId: string): Promise<Comanda> {
    return this.http.requisitar({ metodo: 'GET', caminho: `/interno/comandas/${comandaId}`, contexto });
  }

  abrir(contexto: Contexto, mesaId: string, mesaRotulo: string): Promise<{ comandaId: string; reaproveitada: boolean }> {
    return this.http.requisitar({ metodo: 'POST', caminho: '/interno/comandas', contexto, corpo: { mesaId, mesaRotulo } });
  }

  adicionarItem(contexto: Contexto, comandaId: string, corpo: unknown): Promise<unknown> {
    return this.http.requisitar({ metodo: 'POST', caminho: `/interno/comandas/${comandaId}/itens`, contexto, corpo });
  }

  confirmar(contexto: Contexto, comandaId: string, chaveIdempotencia: string): Promise<unknown> {
    return this.http.requisitar({ metodo: 'POST', caminho: `/interno/comandas/${comandaId}/confirmacao`, contexto, corpo: { chaveIdempotencia } });
  }

  cancelarItem(contexto: Contexto, comandaId: string, itemId: string): Promise<void> {
    return this.http.requisitar({ metodo: 'DELETE', caminho: `/interno/comandas/${comandaId}/itens/${encodeURIComponent(itemId)}`, contexto });
  }
}
