import type { CacheEmMemoria } from '../cache/cacheEmMemoria.js';
import type {
  CardapioAtivo,
  CatalogoClient,
  Comanda,
  EstabelecimentoClient,
  MesaResolvida,
  PedidoClient,
} from '../clientes/servicos.client.js';
import { ErroHttp, type Contexto } from '../contexto.js';

export interface MesaDoCliente {
  contexto: Contexto;
  mesa: MesaResolvida;
}

export interface TelaDaMesa {
  mesa: { rotulo: string; estabelecimento: string; percentualTaxaServico: number };
  cardapio: CardapioAtivo | null;
  comanda: Comanda | null;
  avisos: string[];
}

export interface NovoItem {
  itemCardapioId: string;
  quantidade: number;
  modificadores?: Array<{ grupo: string; opcao: string }>;
  observacao?: string;
}

/**
 * Composicao do bff-mesa (Figura 13). O qrToken e a credencial de escopo limitado:
 * toda operacao usa a comanda ABERTA da mesa resolvida, nunca um comandaId vindo do cliente,
 * o que garante a regra "comanda pertence a mesa do token" (Secao 8.3).
 */
export class MesaComposicao {
  constructor(
    private readonly estabelecimento: EstabelecimentoClient,
    private readonly catalogo: CatalogoClient,
    private readonly pedido: PedidoClient,
    private readonly cacheMesas: CacheEmMemoria<MesaResolvida>,
    private readonly cacheCardapios: CacheEmMemoria<CardapioAtivo>,
  ) {}

  async resolver(qrToken: string, correlationId: string): Promise<MesaDoCliente> {
    if (!/^[A-Za-z0-9_-]{6,120}$/.test(qrToken)) {
      throw new ErroHttp(404, 'QR Code invalido', 'QR Code invalido, chame o atendente');
    }
    const mesa = await this.cacheMesas.obter(qrToken, () => this.estabelecimento.resolverQrToken(qrToken, correlationId))
      .catch((erro: unknown) => {
        if (erro instanceof ErroHttp && erro.status === 404) {
          throw new ErroHttp(404, 'QR Code invalido', 'QR Code invalido, chame o atendente');
        }
        throw erro;
      });
    return {
      mesa,
      contexto: { tenantId: mesa.tenantId, usuarioId: `mesa:${mesa.mesaId}`, papeis: ['MESA'], correlationId },
    };
  }

  /** Uma requisicao do celular, duas consultas em paralelo; falha do pedido nao derruba o cardapio (8.6). */
  async tela(qrToken: string, correlationId: string): Promise<TelaDaMesa> {
    const { mesa, contexto } = await this.resolver(qrToken, correlationId);
    const [cardapio, comanda] = await Promise.allSettled([
      this.cacheCardapios.obter(mesa.tenantId, () => this.catalogo.cardapioAtivo(contexto)),
      this.comandaAberta(contexto, mesa.mesaId),
    ]);
    const avisos: string[] = [];
    if (cardapio.status === 'rejected') avisos.push('cardapio temporariamente indisponivel');
    if (comanda.status === 'rejected') avisos.push('estado da comanda temporariamente indisponivel');
    return {
      mesa: { rotulo: mesa.rotulo, estabelecimento: mesa.nomeEstabelecimento, percentualTaxaServico: mesa.percentualTaxaServico },
      cardapio: cardapio.status === 'fulfilled' ? cardapio.value : null,
      comanda: comanda.status === 'fulfilled' ? comanda.value : null,
      avisos,
    };
  }

  async comandaAberta(contexto: Contexto, mesaId: string): Promise<Comanda | null> {
    const abertas = await this.pedido.comandasAbertasDaMesa(contexto, mesaId);
    return abertas[0] ?? null;
  }

  async exigirComandaAberta(cliente: MesaDoCliente): Promise<Comanda> {
    const comanda = await this.comandaAberta(cliente.contexto, cliente.mesa.mesaId);
    if (!comanda) throw new ErroHttp(404, 'Sem comanda', 'a mesa nao possui comanda aberta');
    return comanda;
  }

  async abrirComanda(cliente: MesaDoCliente): Promise<Comanda> {
    const aberta = await this.pedido.abrir(cliente.contexto, cliente.mesa.mesaId, cliente.mesa.rotulo);
    return this.pedido.obter(cliente.contexto, aberta.comandaId);
  }

  /** Figura 14: abre a comanda na primeira adicao e devolve o carrinho atualizado. */
  async adicionarItem(cliente: MesaDoCliente, item: NovoItem): Promise<Comanda> {
    const comanda = (await this.comandaAberta(cliente.contexto, cliente.mesa.mesaId)) ?? (await this.abrirComanda(cliente));
    await this.pedido.adicionarItem(cliente.contexto, comanda.comandaId, {
      itemCardapioId: item.itemCardapioId,
      quantidade: item.quantidade,
      modificadores: item.modificadores ?? [],
      observacao: item.observacao,
    });
    return this.pedido.obter(cliente.contexto, comanda.comandaId);
  }

  async confirmar(cliente: MesaDoCliente, chaveIdempotencia: string): Promise<unknown> {
    const comanda = await this.exigirComandaAberta(cliente);
    return this.pedido.confirmar(cliente.contexto, comanda.comandaId, chaveIdempotencia);
  }

  async cancelarItem(cliente: MesaDoCliente, itemId: string): Promise<void> {
    const comanda = await this.exigirComandaAberta(cliente);
    if (!comanda.itens.some((i) => i.itemId === itemId)) {
      throw new ErroHttp(404, 'Item nao encontrado', 'o item nao pertence a comanda desta mesa');
    }
    await this.pedido.cancelarItem(cliente.contexto, comanda.comandaId, itemId);
  }
}
