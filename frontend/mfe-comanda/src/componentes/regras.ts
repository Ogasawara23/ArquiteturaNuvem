import type { Grupo } from '../api/bffMesaClient';

export const reais = (centavos: number) => (centavos / 100).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export const ROTULO_STATUS: Record<string, string> = {
  PENDENTE: 'No carrinho',
  CONFIRMADO: 'Enviado a cozinha',
  EM_PRODUCAO: 'Em preparo',
  PRONTO: 'Pronto',
  ENTREGUE: 'Entregue',
  CANCELADO: 'Cancelado',
};

/**
 * Validacao de usabilidade no navegador (feedback imediato). A regra autoritativa
 * continua no agregado Comanda do pedido-service.
 */
export function validarSelecoes(grupos: Grupo[], selecoes: Record<string, string[]>): string | null {
  for (const g of grupos) {
    const n = selecoes[g.nome]?.length ?? 0;
    if (n < g.minimoSelecoes) return `Escolha ${g.minimoSelecoes === 1 ? 'uma opcao' : `${g.minimoSelecoes} opcoes`} em "${g.nome}"`;
    if (n > g.maximoSelecoes) return `No maximo ${g.maximoSelecoes} em "${g.nome}"`;
  }
  return null;
}

export function precoComAdicionais(precoCentavos: number, grupos: Grupo[], selecoes: Record<string, string[]>, quantidade: number): number {
  const adicionais = grupos.flatMap((g) => g.opcoes.filter((o) => selecoes[g.nome]?.includes(o.nome)))
    .reduce((s, o) => s + o.precoAdicionalCentavos, 0);
  return (precoCentavos + adicionais) * quantidade;
}
