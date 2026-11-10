import { describe, expect, it } from 'vitest';
import { escreverGrupos, lerGrupos } from '../src/componentes/gruposEmTexto';

describe('editor de modificadores em texto', () => {
  it('le grupos obrigatorios, faixas, precos e opcoes esgotadas', () => {
    const grupos = lerGrupos('Ponto da carne* (1-1): Ao ponto, Bem passado\nAdicionais (0-3): Bacon extra=6.00, Cheddar extra=4.00!');
    expect(grupos[0]).toMatchObject({ nome: 'Ponto da carne', obrigatorio: true, minimoSelecoes: 1, maximoSelecoes: 1 });
    expect(grupos[1].opcoes).toEqual([
      { nome: 'Bacon extra', precoAdicionalCentavos: 600, disponivel: true },
      { nome: 'Cheddar extra', precoAdicionalCentavos: 400, disponivel: false },
    ]);
  });

  it('ida e volta preserva o conteudo', () => {
    const texto = 'Ponto* (1-1): Ao ponto, Bem passado\nAdicionais (0-2): Bacon=6.00, Ovo!';
    expect(escreverGrupos(lerGrupos(texto))).toBe(texto);
  });

  it('linha mal formada gera erro explicativo', () => {
    expect(() => lerGrupos('sem formato')).toThrow(/linha invalida/);
  });
});
