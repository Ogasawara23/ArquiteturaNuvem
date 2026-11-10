# pedido-service

**Microsserviço 2 — Pedido / Comanda (Azure SQL Database)** — parte do [SmartKDS & Menu](../../README.md).

Dono do agregado `Comanda`: abre a comanda da mesa, adiciona itens **congelando preço e descrição**, confirma o pedido gravando `PedidoConfirmado` na **Transactional Outbox** (mesma transação do agregado), cancela itens (saga coreografada), altera itens do carrinho, cancela ou fecha a comanda e mantém a auditoria. É o **Microsserviço 2** da entrega: **Azure SQL Database**, CRUD completo e Swagger. Consome `TicketStatusAlterado` e `TicketExpedido` de forma idempotente (inbox).

Publica `PedidoConfirmado`, `ItemCancelado` e `ComandaFechada` no exchange `smartkds.pedidos` pelo `OutboxRelay` (polling de 200 ms, `FOR UPDATE SKIP LOCKED`, publisher confirms). Migrações por fornecedor (`db/migration/sqlserver` para o Azure SQL, padrão; `db/migration/postgresql` como alternativa). Row Level Security em `comanda` e `log_auditoria`: `SECURITY POLICY` + `SESSION_CONTEXT` no Azure SQL (RLS no PostgreSQL). A trava da outbox usa `SKIP LOCKED`/`READPAST` de forma portável.

## Arquitetura do código: Vertical Slice + Clean Architecture

```
br/com/smartkds/pedido/
├── features/            uma fatia por funcionalidade
│   ├── abrircomanda/
│   ├── adicionaritem/
│   ├── alteraritemdacomanda/
│   ├── cancelarcomanda/
│   ├── cancelaritem/
│   ├── confirmarpedido/
│   ├── consultarauditoria/
│   ├── consultarcomanda/
│   ├── fecharcomanda/
│   ├── registrarentregadopedido/
│   ├── registrareventorejeitado/
│   ├── registrarprogressodaproducao/
├── shared/              núcleo compartilhado do contexto
│   ├── dominio/
│   ├── eventos/
│   ├── inbox/
│   ├── integracao/
│   ├── outbox/
│   ├── persistencia/
│   ├── tenancy/
│   ├── web/
└── config/              composição Spring, mensageria e OpenAPI
```

| Camada (Clean Architecture) | Onde fica |
|---|---|
| **Domain** | `shared/dominio`: agregados, value objects e regras em **Java puro** (sem Spring, JPA, Jackson ou AMQP) |
| **Application** | `features/<fatia>/*Handler` ou `*Query`: orquestra o caso de uso; a regra mora no agregado |
| **Infrastructure** | `shared/persistencia`, `outbox`, `inbox`, `integracao`, `tenancy`: adaptadores das portas |
| **API** | `features/<fatia>/*Endpoint` (HTTP) e `*Listener` (AMQP), com `Request`/`Response`/`Command` locais à fatia |

As regras são verificadas a cada build por `ArquiteturaVerticalSliceTest` (ArchUnit): fatias não dependem umas das outras,
domínio é Java puro, `shared` não conhece `features`, um único Handler/Query por fatia, classes da fatia com visibilidade de
pacote, repositórios recebem `TenantId` como primeiro parâmetro e nenhum acesso ao banco de outro serviço.

## Endpoints internos (consumidos apenas pelos BFFs)

As rotas HTTP exigem o cabeçalho `X-Tenant-Id`, injetado pelo API Gateway/BFF, salvo indicação.

| Método | Rota / fila | Fatia |
|---|---|---|
| POST | `/interno/comandas` | `abrircomanda` |
| POST | `/interno/comandas/{id}/itens` | `adicionaritem` |
| PATCH | `/interno/comandas/{id}/itens/{itemId}` | `alteraritemdacomanda` |
| DELETE | `/interno/comandas/{id}` | `cancelarcomanda` |
| POST | `/interno/comandas/{id}/confirmacao` | `confirmarpedido` |
| DELETE | `/interno/comandas/{id}/itens/{itemId}` | `cancelaritem` |
| GET | `/interno/comandas[/{id}]` | `consultarcomanda` |
| POST | `/interno/comandas/{id}/fechamento` | `fecharcomanda` |
| GET | `/interno/auditoria` | `consultarauditoria` |
| AMQP | `pedido.producao (ticket.expedido)` | `registrarentregadopedido` |
| AMQP | `pedido.producao.status (ticket.status)` | `registrarprogressodaproducao` |
| AMQP | `*.dlq` | `registrareventorejeitado` |

Swagger: `http://localhost:8082/swagger-ui.html` (direto) ou `<api-gateway>/docs/pedido/swagger-ui.html` (publicado).

## Tecnologias

Java 21 · Spring Boot 3.5 · Spring Web · Spring Data JPA · Flyway · Azure SQL Database (SQL Server, RLS) · PostgreSQL 16 (alternativa) · Spring AMQP (RabbitMQ) · springdoc-openapi · JUnit 5 · ArchUnit · Testcontainers (SQL Server) e PostgreSQL embutido nos testes de integração

## Variáveis de ambiente

| Variável | Padrão local | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:sqlserver://localhost:1433;databaseName=smartkds_pedido;encrypt=false` | Azure SQL: `jdbc:sqlserver://<srv>.database.windows.net:1433;databaseName=smartkds_pedido;encrypt=true` |
| `DB_USER / DB_PASSWORD` | `sa / SmartKds!2026` | Credenciais do Azure SQL (local: SQL Server 2022 do docker-compose) |
| `RABBITMQ_URL` | `amqp://guest:guest@localhost:5672` | Broker (CloudAMQP: `amqps://...`) |
| `CATALOGO_URL` | `http://localhost:8081` | catalogo-service (snapshot do item) |

## Como rodar localmente

```bash
docker compose up -d sqlserver sqlserver-init rabbitmq   # na raiz do monorepo
mvn spring-boot:run
mvn test   # unitários + ArchUnit + integração com PostgreSQL embutido; com Docker disponível, também contra SQL Server (Testcontainers)
```

## Docker

```bash
docker build -t <usuario>/smartkds-pedido-service:v1 .
docker push <usuario>/smartkds-pedido-service:v1
```

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
