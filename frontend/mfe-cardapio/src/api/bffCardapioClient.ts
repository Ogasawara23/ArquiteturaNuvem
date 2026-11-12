import type { Sessao } from './sessao';

export interface Opcao { nome: string; precoAdicionalCentavos: number; disponivel: boolean }
export interface Grupo { nome: string; obrigatorio: boolean; minimoSelecoes: number; maximoSelecoes: number; opcoes: Opcao[] }

export interface ItemResumo {
  id: string;
  nome: string;
  categoria: string;
  precoCentavos: number;
  estacaoSugerida: string;
  tempoAlvoPreparoSegundos: number;
  disponivel: boolean;
}

export interface DadosDoItem {
  nome: string;
  descricao?: string;
  categoria: string;
  precoCentavos: number;
  estacaoSugerida: string;
  tempoAlvoPreparoSegundos: number;
  imagemUrl?: string;
  gruposModificadores: Grupo[];
}

export interface ItemCompleto extends DadosDoItem { id: string; disponivel: boolean }

/** Cliente do bff-admin (rotas /bff/admin/cardapio/**). */
export function criarClienteCardapio(sessao: Sessao) {
  const base = `${sessao.apiUrl.replace(/\/$/, '')}/bff/admin`;
  async function chamar<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
    const h: Record<string, string> = corpo === undefined ? {} : { 'content-type': 'application/json' };
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
    listar: () => chamar<ItemResumo[]>('GET', '/cardapio/itens'),
    obter: (id: string) => chamar<ItemCompleto>('GET', `/cardapio/itens/${id}`),
    cadastrar: (d: DadosDoItem) => chamar<{ id: string }>('POST', '/cardapio/itens', d),
    atualizar: (id: string, d: DadosDoItem) => chamar<unknown>('PUT', `/cardapio/itens/${id}`, d),
    disponibilidade: (id: string, disponivel: boolean) => chamar<unknown>('PATCH', `/cardapio/itens/${id}/disponibilidade`, { disponivel }),
    remover: (id: string) => chamar<void>('DELETE', `/cardapio/itens/${id}`),
    publicar: () => chamar<{ versaoId: string; quantidadeDeItens: number }>('POST', '/cardapio/versoes', { publicadaPor: sessao.usuario }),
    ativo: () => chamar<{ versaoId: string; publicadaEm: string }>('GET', '/cardapio/ativo'),
  };
}
