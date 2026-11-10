/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL?: string;
  readonly VITE_REMOTE_COMANDA?: string;
}

declare module 'comanda/TelaComanda' {
  import type { ComponentType } from 'react';
  const Tela: ComponentType<{ apiUrl: string; qrToken: string }>;
  export default Tela;
}
