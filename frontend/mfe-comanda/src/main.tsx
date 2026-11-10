import { createRoot } from 'react-dom/client';
import TelaComanda from './TelaComanda';

/** Execucao isolada do remote (desenvolvimento): no produto ele e carregado pelo shell-mesa. */
createRoot(document.getElementById('raiz')!).render(
  <TelaComanda apiUrl={import.meta.env.VITE_API_URL ?? 'http://localhost:8080'} qrToken={new URLSearchParams(location.search).get('qr') ?? 'demo-mesa-1'} />,
);
