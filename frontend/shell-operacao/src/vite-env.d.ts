/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL?: string;
  readonly VITE_URL_MESA?: string;
  readonly VITE_KEYCLOAK_URL?: string;
  readonly VITE_KEYCLOAK_CLIENT_ID?: string;
  readonly VITE_REMOTE_KDS?: string;
  readonly VITE_REMOTE_CARDAPIO?: string;
  readonly VITE_REMOTE_GESTAO?: string;
}
