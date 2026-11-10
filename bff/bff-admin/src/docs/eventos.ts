import { readFile } from 'node:fs/promises';
import type { FastifyInstance } from 'fastify';

/**
 * "Swagger dos eventos": publica o contrato AsyncAPI 3.0 dos eventos de dominio em
 * /asyncapi.yaml e uma visualizacao navegavel em /eventos (via gateway: /bff/admin/eventos).
 */
export function registrarDocumentacaoDeEventos(app: FastifyInstance, arquivo: string): void {
  app.get('/asyncapi.yaml', { schema: { hide: true } }, async (_req, reply) => {
    reply.type('application/yaml; charset=utf-8').send(await readFile(arquivo, 'utf-8'));
  });
  app.get('/eventos', { schema: { hide: true } }, async (_req, reply) => {
    reply.type('text/html; charset=utf-8').send(`<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>SmartKDS & Menu - Eventos (AsyncAPI)</title>
  <link rel="stylesheet" href="https://unpkg.com/@asyncapi/react-component@1.4.10/styles/default.min.css" />
</head>
<body>
  <div id="asyncapi"></div>
  <script src="https://unpkg.com/@asyncapi/react-component@1.4.10/browser/standalone/index.js"></script>
  <script>
    const base = location.pathname.replace(/\\/eventos\\/?$/, '');
    AsyncApiStandalone.render({ schema: { url: base + '/asyncapi.yaml' }, config: { show: { sidebar: true } } },
      document.getElementById('asyncapi'));
  </script>
</body>
</html>`);
  });
}
