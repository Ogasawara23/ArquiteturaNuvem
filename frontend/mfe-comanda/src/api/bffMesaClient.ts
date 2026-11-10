export interface Opcao { nome: string; precoAdicionalCentavos: number; disponivel: boolean }
export interface Grupo { nome: string; obrigatorio: boolean; minimoSelecoes: number; maximoSelecoes: number; opcoes: Opcao[] }
export interface ItemDoCardapio {
  itemCardapioId: string;
  nome: string;
  descricao: string | null;
  categoria: string;
  precoCentavos: number;
  imagemUrl: string | null;
  gruposModificadores: Grupo[];
}
export interface ItemDaComanda {
  itemId: string;
  descricao: string;
  quantidade: number;
  totalCentavos: number;
  modificadores: string[];
  observacao: string | null;
  status: 'PENDENTE' | 'CONFIRMADO' | 'EM_PRODUCAO' | 'PRONTO' | 'ENTREGUE' | 'CANCELADO';
  podeSerCancelado: boolean;
}
export interface Comanda { comandaId: string; mesaRotulo: string; status: string; subtotalCentavos: number; itens: ItemDaComanda[] }
export interface TelaDaMesa {
  mesa: { rotulo: string; estabelecimento: string; percentualTaxaServico: number };
  cardapio: { versaoId: string; categorias: Array<{ nome: string; itens: ItemDoCardapio[] }> } | null;
  comanda: Comanda | null;
  avisos: string[];
}
export interface NovoItem {
  itemCardapioId: string;
  quantidade: number;
  modificadores: Array<{ grupo: string; opcao: string }>;
  observacao?: string;
}

/** Cliente do bff-mesa. A unica credencial e o qrToken da URL (sem login, LGPD: a comanda identifica a mesa). */
export function criarClienteMesa(apiUrl: string, qrToken: string) {
  const base = `${apiUrl.replace(/\/$/, '')}/bff/mesa/${encodeURIComponent(qrToken)}`;
  async function chamar<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
    const r = await fetch(`${base}${caminho}`, {
      method: metodo,
      headers: { 'content-type': 'application/json' },
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });
    if (!r.ok) {
      const p = await r.json().catch(() => ({}));
      throw new Error(p.detail ?? 'Nao foi possivel concluir. Tente novamente.');
    }
    return (r.status === 204 ? undefined : await r.json()) as T;
  }
  return {
    tela: () => chamar<TelaDaMesa>('GET', '/cardapio'),
    comanda: () => chamar<Comanda | undefined>('GET', '/comanda'),
    adicionar: (item: NovoItem) => chamar<Comanda>('POST', '/comanda/itens', item),
    confirmar: (chaveIdempotencia: string) => chamar<unknown>('POST', '/comanda/confirmacao', { chaveIdempotencia }),
    cancelar: (itemId: string) => chamar<void>('DELETE', `/comanda/itens/${encodeURIComponent(itemId)}`),
    urlDoStream: () => `${base}/comanda/stream`,
  };
}
