import type { Grupo } from '../api/bffCardapioClient';

/**
 * Editor compacto de modificadores, uma linha por grupo:
 *   Ponto da carne* (1-1): Ao ponto, Bem passado
 *   Adicionais (0-3): Bacon extra=6.00, Cheddar extra=4.00
 * "*" marca grupo obrigatorio; "=preco" e o adicional em reais; "!" no fim marca opcao esgotada.
 */
export function lerGrupos(texto: string): Grupo[] {
  return texto.split('\n').map((l) => l.trim()).filter(Boolean).map((linha) => {
    const m = /^(.+?)(\*)?\s*\((\d+)\s*-\s*(\d+)\)\s*:\s*(.+)$/.exec(linha);
    if (!m) throw new Error(`linha invalida: "${linha}" (use: Nome* (min-max): opcao, opcao=preco)`);
    const opcoes = m[5].split(',').map((o) => o.trim()).filter(Boolean).map((o) => {
      const esgotada = o.endsWith('!');
      const [nome, preco] = o.replace(/!$/, '').split('=').map((x) => x.trim());
      return { nome, precoAdicionalCentavos: Math.round(Number((preco ?? '0').replace(',', '.')) * 100), disponivel: !esgotada };
    });
    return { nome: m[1].trim(), obrigatorio: Boolean(m[2]), minimoSelecoes: Number(m[3]), maximoSelecoes: Number(m[4]), opcoes };
  });
}

export function escreverGrupos(grupos: Grupo[]): string {
  return grupos.map((g) => `${g.nome}${g.obrigatorio ? '*' : ''} (${g.minimoSelecoes}-${g.maximoSelecoes}): ${g.opcoes
    .map((o) => `${o.nome}${o.precoAdicionalCentavos ? `=${(o.precoAdicionalCentavos / 100).toFixed(2)}` : ''}${o.disponivel ? '' : '!'}`)
    .join(', ')}`).join('\n');
}

export const reais = (centavos: number) => (centavos / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
