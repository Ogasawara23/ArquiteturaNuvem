# bff-mesa

**BFF do cliente na mesa** — Backend for Frontend do [SmartKDS & Menu](../../README.md) (Node.js 22 + Fastify 5 + TypeScript).

Resolve o `qrToken` em mesa e tenant, compõe **cardápio ativo + comanda aberta em paralelo** numa única resposta (Figura 13), abre a comanda na primeira adição (Figura 14) e mantém o stream SSE da comanda. Toda operação usa a comanda aberta da mesa resolvida, nunca um `comandaId` vindo do cliente. Se o pedido-service cair, o cardápio continua sendo servido com um aviso (degradação parcial).

O BFF **não** contém regra de negócio nem banco de dados (ADR-03): toda decisão de domínio é delegada a uma fatia de um
microsserviço. Fica atrás do API Gateway (`/bff/mesa/**`) e chama os microsserviços pela rede interna (ADR-04).

## Rotas públicas (via API Gateway em `/bff/mesa`)

| Método | Rota | Compõe / encaminha para |
|---|---|---|
| GET | `/{qrToken}/cardapio` | catalogo + estabelecimento + pedido (paralelo) |
| GET | `/{qrToken}/comanda` | pedido |
| POST | `/{qrToken}/comanda` | pedido |
| POST | `/{qrToken}/comanda/itens` | pedido (+ catálogo, no serviço) |
| POST | `/{qrToken}/comanda/confirmacao` | pedido (Outbox) |
| DELETE | `/{qrToken}/comanda/itens/{itemId}` | pedido |
| GET | `/{qrToken}/comanda/stream` | SSE alimentado pelo RabbitMQ |

Swagger (OpenAPI 3.1): `http://localhost:3001/docs` ou `<api-gateway>/bff/mesa/docs` · documento em `/openapi.json`.

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

`CATALOGO_URL`, `PEDIDO_URL`, `ESTABELECIMENTO_URL`, `RABBITMQ_URL` (opcional: sem ele não há tempo real), `PORT`=3001

## Rodar e testar

```bash
npm install
npm run dev        # http://localhost:3001
npm test           # Vitest: unitários, rotas (fastify.inject) e arquitetura
npm run build && npm start
docker build -t <usuario>/smartkds-bff-mesa:v1 . && docker push <usuario>/smartkds-bff-mesa:v1
```

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
