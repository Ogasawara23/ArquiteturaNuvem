import federation from '@originjs/vite-plugin-federation';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// Remote de Module Federation: build e deploy proprios, carregado em runtime pelo shell-operacao.
export default defineConfig({
  plugins: [
    react(),
    federation({
      name: 'kds',
      filename: 'remoteEntry.js',
      exposes: { './TelaProducao': './src/TelaProducao.tsx' },
      shared: ['react', 'react-dom'],
    }),
  ],
  build: { target: 'esnext', minify: true, cssCodeSplit: false, modulePreload: false },
  preview: { cors: true },
  test: { environment: 'jsdom' },
});
