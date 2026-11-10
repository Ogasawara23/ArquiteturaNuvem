import { lazy, type ComponentType } from 'react';
import type { Sessao } from '../sessao/ContextoDeSessao';

export interface Remote {
  id: string;
  titulo: string;
  papeis: string[];
  Componente: ComponentType<{ sessao: Sessao }>;
}

/** Import dinamico dos modulos expostos (import('kds/TelaProducao') etc., Figura 6). */
export const REMOTES: Remote[] = [
  { id: 'kds', titulo: 'Cozinha (KDS)', papeis: ['COZINHA', 'GERENTE', 'ADMIN'], Componente: lazy(() => import('kds/TelaProducao')) },
  { id: 'cardapio', titulo: 'Cardapio', papeis: ['GERENTE', 'ADMIN'], Componente: lazy(() => import('cardapio/TelaCardapio')) },
  { id: 'gestao', titulo: 'Gestao', papeis: ['GERENTE', 'ADMIN', 'ATENDENTE'], Componente: lazy(() => import('gestao/TelaGestao')) },
];

export function remotesPermitidos(papeis: string[]): Remote[] {
  return REMOTES.filter((r) => r.papeis.some((p) => papeis.includes(p)));
}
