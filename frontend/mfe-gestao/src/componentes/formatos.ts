import type { Roteamento } from '../api/bffGestaoClient';

export const reais = (centavos: number | null | undefined) =>
  ((centavos ?? 0) / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export const minutos = (segundos: number | null | undefined) =>
  segundos == null ? '-' : `${Math.floor(segundos / 60)}min ${String(Math.round(segundos % 60)).padStart(2, '0')}s`;

/** Regras em texto: "categoria:Bebidas -> bar" ou "item:<id> -> grill (5)". Primeira linha opcional "padrao -> cozinha". */
export function lerRegras(texto: string): Roteamento {
  let estacaoPadrao: string | null = null;
  const regras: Roteamento['regras'] = [];
  for (const linha of texto.split('\n').map((l) => l.trim()).filter(Boolean)) {
    const m = /^(padrao|categoria:(.+?)|item:(.+?))\s*->\s*([\w-]+)\s*(?:\((\d+)\))?$/i.exec(linha);
    if (!m) throw new Error(`regra invalida: "${linha}"`);
    if (m[1].toLowerCase() === 'padrao') estacaoPadrao = m[4];
    else regras.push({ categoria: m[2]?.trim() ?? null, itemCardapioId: m[3]?.trim() ?? null, estacaoId: m[4], prioridade: Number(m[5] ?? 0) });
  }
  return { estacaoPadrao, regras };
}

export function escreverRegras(r: Roteamento): string {
  return [
    ...(r.estacaoPadrao ? [`padrao -> ${r.estacaoPadrao}`] : []),
    ...r.regras.map((x) => `${x.categoria ? `categoria:${x.categoria}` : `item:${x.itemCardapioId}`} -> ${x.estacaoId}${x.prioridade ? ` (${x.prioridade})` : ''}`),
  ].join('\n');
}
