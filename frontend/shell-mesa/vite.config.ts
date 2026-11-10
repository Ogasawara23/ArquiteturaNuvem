import federation from '@originjs/vite-plugin-federation';
import react from '@vitejs/plugin-react';
import { loadEnv } from 'vite';
import { defineConfig } from 'vitest/config';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    plugins: [
      react(),
      federation({
        name: 'shell_mesa',
        remotes: { comanda: env.VITE_REMOTE_COMANDA ?? 'http://localhost:5101/assets/remoteEntry.js' },
        shared: ['react', 'react-dom'],
      }),
    ],
    build: { target: 'esnext', modulePreload: false },
    test: { environment: 'jsdom' },
  };
});
