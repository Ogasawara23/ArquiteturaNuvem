// Declaracao dos modulos expostos pelos remotes (contrato de Module Federation).
declare module 'kds/TelaProducao' {
  import type { ComponentType } from 'react';
  const Tela: ComponentType<{ sessao: import('../sessao/ContextoDeSessao').Sessao }>;
  export default Tela;
}
declare module 'cardapio/TelaCardapio' {
  import type { ComponentType } from 'react';
  const Tela: ComponentType<{ sessao: import('../sessao/ContextoDeSessao').Sessao }>;
  export default Tela;
}
declare module 'gestao/TelaGestao' {
  import type { ComponentType } from 'react';
  const Tela: ComponentType<{ sessao: import('../sessao/ContextoDeSessao').Sessao }>;
  export default Tela;
}
