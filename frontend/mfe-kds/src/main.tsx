import { createRoot } from 'react-dom/client';
import TelaProducao from './TelaProducao';

/** Execucao isolada do remote (desenvolvimento): no produto ele e carregado pelo shell-operacao. */
createRoot(document.getElementById('raiz')!).render(
  <TelaProducao sessao={{ apiUrl: import.meta.env.VITE_API_URL ?? 'http://localhost:8080', tenantId: 'demo', usuario: 'dev', papeis: ['COZINHA'] }} />,
);
