import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AppMesa } from './AppMesa';
import './design-system/tokens.css';
import './design-system/mesa.css';

createRoot(document.getElementById('raiz')!).render(
  <StrictMode>
    <AppMesa />
  </StrictMode>,
);
