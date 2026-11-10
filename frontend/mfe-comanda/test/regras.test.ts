import { describe, expect, it } from 'vitest';
import type { Grupo } from '../src/api/bffMesaClient';
import { precoComAdicionais, validarSelecoes } from '../src/componentes/regras';

const grupos: Grupo[] = [
  { nome: 'Ponto', obrigatorio: true, minimoSelecoes: 1, maximoSelecoes: 1, opcoes: [{ nome: 'Ao ponto', precoAdicionalCentavos: 0, disponivel: true }] },
  { nome: 'Adicionais', obrigatorio: false, minimoSelecoes: 0, maximoSelecoes: 2, opcoes: [{ nome: 'Bacon', precoAdicionalCentavos: 600, disponivel: true }, { nome: 'Ovo', precoAdicionalCentavos: 300, disponivel: true }] },
];

describe('regras do carrinho no navegador', () => {
  it('exige grupo obrigatorio', () => {
    expect(validarSelecoes(grupos, {})).toMatch(/Ponto/);
    expect(validarSelecoes(grupos, { Ponto: ['Ao ponto'] })).toBeNull();
  });

  it('respeita o maximo de selecoes', () => {
    expect(validarSelecoes(grupos, { Ponto: ['Ao ponto'], Adicionais: ['Bacon', 'Ovo', 'X'] })).toMatch(/maximo/);
  });

  it('preco exibido soma adicionais e multiplica pela quantidade', () => {
    expect(precoComAdicionais(4200, grupos, { Adicionais: ['Bacon', 'Ovo'] }, 2)).toBe((4200 + 900) * 2);
  });
});
