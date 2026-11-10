# bff-kds

**BFF da tela de produção (KDS)** — Backend for Frontend do [SmartKDS & Menu](../../README.md) (Node.js 22 + Fastify 5 + TypeScript).

Agrega os tickets da estação (producao-service) com o SLA da estação (estabelecimento-service, cache de 60 s), calcula o destaque `normal`/`atencao`/`atrasado` no servidor e mantém o stream SSE por estação (componentes da Figura 5: rotas, composição, clientes, cache, sse, eventos).

O BFF **não** contém regra de negócio nem banco de dados (ADR-03): toda decisão de domínio é delegada a uma fatia de um
microsserviço. Fica atrás do API Gateway (`/bff/kds/**`) e chama os microsserviços pela rede interna (ADR-04).

## Rotas públicas (via API Gateway em `/bff/kds`)

| Método | Rota | Compõe / encaminha para |
|---|---|---|
| GET | `/estacoes` | estabelecimento |
| GET | `/estacoes/{estacao}/tickets` | producao + estabelecimento |
| PATCH | `/tickets/{id}/status` | producao |
| POST | `/tickets/{id}/bump` | producao (Outbox) |
| GET | `/estacoes/{estacao}/stream` | SSE: ticket-criado, ticket-atualizado, ticket-expedido, fila-alterada |

Swagger (OpenAPI 3.1): `http://localhost:3002/docs` ou `<api-gateway>/bff/kds/docs` · documento em `/openapi.json`.

## Organização do código

```
src/
├── rotas/        entrada HTTP (validação por JSON Schema, papéis via X-User-Roles)
├── composicao/   montagem dos view models (paralelismo, degradação parcial)
├── clientes/     ClienteHttpResiliente: timeout, nova tentativa em GET, circuit breaker por dependência
├── cache/        cache em memória com TTL e fallback ao último valor
├── sse/          GerenciadorDeStreams (conexões por tenant e chave)
├── eventos/      consumidor AMQP (fila exclusiva por réplica)
├── docs/         OpenAPI + Swagger UI
└── servidor.ts   composição (único ponto que conhece todas as peças)
```

Testes de arquitetura (`test/arquitetura.test.ts`): `composicao` não conhece Fastify nem rotas; `clientes` não dependem de
`composicao`/`rotas`/`sse`; `sse` e `eventos` não chamam microsserviços; nenhum driver de banco; nenhum import de outro BFF.

## Variáveis de ambiente

`PRODUCAO_URL`, `ESTABELECIMENTO_URL`, `RABBITMQ_URL`, `PORT`=3002

## Rodar e testar

```bash
npm install
npm run dev        # http://localhost:3002
npm test           # Vitest: unitários, rotas (fastify.inject) e arquitetura
npm run build && npm start
docker build -t <usuario>/smartkds-bff-kds:v1 . && docker push <usuario>/smartkds-bff-kds:v1
```

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
