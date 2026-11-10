# bff-admin

**BFF das telas administrativas** — Backend for Frontend do [SmartKDS & Menu](../../README.md) (Node.js 22 + Fastify 5 + TypeScript).

Traz o endpoint obrigatório **`GET /aggregated-data`**: chama o **Microsserviço 1** (catalogo-service, MongoDB), o **Microsserviço 2** (pedido-service, Azure SQL) e a **Azure Function** status-operacional **em paralelo** (mais os indicadores do producao-service) e devolve tudo num único response, com a situação e a latência de cada fonte. Também faz o proxy das operações CRUD administrativas e publica a documentação dos eventos (AsyncAPI).

O BFF **não** contém regra de negócio nem banco de dados (ADR-03): toda decisão de domínio é delegada a uma fatia de um
microsserviço. Fica atrás do API Gateway (`/bff/admin/**`) e chama os microsserviços pela rede interna (ADR-04).

## Rotas públicas (via API Gateway em `/bff/admin`)

| Método | Rota | Compõe / encaminha para |
|---|---|---|
| GET | `/aggregated-data` | catalogo + pedido + Azure Function + producao |
| GET/POST | `/cardapio/itens` | catalogo |
| GET/PUT/DELETE | `/cardapio/itens/{id}` | catalogo |
| PATCH | `/cardapio/itens/{id}/disponibilidade` | catalogo |
| POST | `/cardapio/versoes` | catalogo |
| GET | `/comandas, /comandas/{id}` | pedido |
| POST/PATCH/DELETE | `/comandas, /comandas/{id}/itens[/{itemId}], /comandas/{id}` | pedido (CRUD da comanda) |
| POST | `/comandas/{id}/fechamento` | pedido |
| GET | `/auditoria` | pedido |
| GET | `/indicadores` | producao |
| GET/PUT | `/estabelecimento, /roteamento` | estabelecimento |
| GET/POST | `/mesas, /estacoes` | estabelecimento |
| GET | `/eventos, /asyncapi.yaml` | documentação dos eventos |

Swagger (OpenAPI 3.1): `http://localhost:3003/docs` ou `<api-gateway>/bff/admin/docs` · documento em `/openapi.json`.

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

`CATALOGO_URL`, `PEDIDO_URL`, `PRODUCAO_URL`, `ESTABELECIMENTO_URL`, `FUNCTION_STATUS_URL`, `FUNCTION_KEY` (opcional), `PORT`=3003

## Rodar e testar

```bash
npm install
npm run dev        # http://localhost:3003
npm test           # Vitest: unitários, rotas (fastify.inject) e arquitetura
npm run build && npm start
docker build -t <usuario>/smartkds-bff-admin:v1 . && docker push <usuario>/smartkds-bff-admin:v1
```

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
