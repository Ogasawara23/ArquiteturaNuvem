import swagger from '@fastify/swagger';
import type { FastifyInstance } from 'fastify';

/**
 * Contrato publico do BFF em OpenAPI 3.1, gerado a partir dos schemas das rotas.
 * /openapi.json entrega o documento e /docs o Swagger UI. Os caminhos sao relativos para
 * funcionar tanto direto quanto atras do API Gateway (ex.: /bff/kds/docs).
 */
export async function registrarDocumentacao(app: FastifyInstance, titulo: string, descricao: string, prefixo: string) {
  await app.register(swagger, {
    openapi: {
      openapi: '3.1.0',
      info: { title: titulo, description: descricao, version: '1.0.0' },
      servers: [{ url: prefixo || '/', description: 'API Gateway' }],
      components: {
        securitySchemes: { bearer: { type: 'http', scheme: 'bearer', bearerFormat: 'JWT' } },
      },
    },
  });
  app.get('/openapi.json', { schema: { hide: true } }, async () => app.swagger());
  app.get('/docs', { schema: { hide: true } }, async (_req, reply) => {
    reply.type('text/html; charset=utf-8').send(paginaSwagger(titulo));
  });
}

function paginaSwagger(titulo: string): string {
  return `<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>${titulo}</title>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.17.14/swagger-ui.css" />
</head>
<body>
  <div id="swagger-ui"></div>
  <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.17.14/swagger-ui-bundle.js"></script>
  <script>
    const base = location.pathname.replace(/\\/docs\\/?$/, '');
    window.ui = SwaggerUIBundle({ url: base + '/openapi.json', dom_id: '#swagger-ui' });
  </script>
</body>
</html>`;
}
