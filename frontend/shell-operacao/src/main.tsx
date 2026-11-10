import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AppRouter } from './AppRouter';
import './design-system/tokens.css';
import './design-system/shell.css';
import { ProvedorDeSessao } from './sessao/ContextoDeSessao';

createRoot(document.getElementById('raiz')!).render(
  <StrictMode>
    <ProvedorDeSessao>
      <AppRouter />
    </ProvedorDeSessao>
  </StrictMode>,
);
