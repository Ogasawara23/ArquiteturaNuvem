import federation from '@originjs/vite-plugin-federation';
import react from '@vitejs/plugin-react';
import { loadEnv } from 'vite';
import { defineConfig } from 'vitest/config';

// Host de Module Federation. As URLs dos remotes (CDN em producao) sao definidas no build.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    plugins: [
      react(),
      federation({
        name: 'shell_operacao',
        remotes: {
          kds: env.VITE_REMOTE_KDS ?? 'http://localhost:5201/assets/remoteEntry.js',
          cardapio: env.VITE_REMOTE_CARDAPIO ?? 'http://localhost:5202/assets/remoteEntry.js',
          gestao: env.VITE_REMOTE_GESTAO ?? 'http://localhost:5203/assets/remoteEntry.js',
        },
        shared: ['react', 'react-dom'],
      }),
    ],
    build: { target: 'esnext', modulePreload: false },
    test: { environment: 'jsdom' },
  };
});
