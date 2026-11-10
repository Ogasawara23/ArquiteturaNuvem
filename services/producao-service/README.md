# producao-service

**Produção / Kitchen Display System (PostgreSQL)** — parte do [SmartKDS & Menu](../../README.md).

Dono do agregado `TicketProducao`. Consome `PedidoConfirmado`, roteia cada item para a estação (regra do item → categoria → estação sugerida → padrão, Seção 8.5) e cria um ticket por estação. Controla RECEBIDO → EM_PREPARO → PRONTO → EXPEDIDO, calcula SLA e indicadores, compensa cancelamentos e publica `TicketCriado`, `TicketStatusAlterado` e `TicketExpedido`.

A única chamada síncrona entre microsserviços é a leitura de configuração no estabelecimento-service, com cache de 5 min e degradação controlada (Seção 4.5).

## Arquitetura do código: Vertical Slice + Clean Architecture

```
br/com/smartkds/producao/
├── features/            uma fatia por funcionalidade
│   ├── avancarstatusticket/
│   ├── cancelarticketsdoitem/
│   ├── consultarindicadores/
│   ├── expedirticket/
│   ├── gerartickets/
│   ├── listarticketsdaestacao/
│   ├── registrareventorejeitado/
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
| AMQP | `producao.pedidos (pedido.confirmado)` | `gerartickets` |
| AMQP | `producao.cancelamentos (pedido.item.cancelado)` | `cancelarticketsdoitem` |
| GET | `/interno/estacoes/{estacao}/tickets` | `listarticketsdaestacao` |
| PATCH | `/interno/tickets/{id}/status` | `avancarstatusticket` |
| POST | `/interno/tickets/{id}/bump` | `expedirticket` |
| GET | `/interno/indicadores?horas=24` | `consultarindicadores` |
| AMQP | `*.dlq` | `registrareventorejeitado` |

Swagger: `http://localhost:8083/swagger-ui.html` (direto) ou `<api-gateway>/docs/producao/swagger-ui.html` (publicado).

## Tecnologias

Java 21 · Spring Boot 3.5 · Spring Web · Spring Data JPA · Flyway · PostgreSQL 16 (RLS) · Spring AMQP (RabbitMQ) · springdoc-openapi · JUnit 5 · ArchUnit · PostgreSQL embutido (zonky) nos testes de integração

## Variáveis de ambiente

| Variável | Padrão local | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/smartkds_producao` |  |
| `DB_USER / DB_PASSWORD` | `producao / producao` |  |
| `RABBITMQ_URL` | `amqp://guest:guest@localhost:5672` |  |
| `ESTABELECIMENTO_URL` | `http://localhost:8084` | Regras de roteamento |

## Como rodar localmente

```bash
docker compose up -d postgres rabbitmq   # na raiz do monorepo
mvn spring-boot:run
mvn test   # unitários + ArchUnit + integração com PostgreSQL real embutido (sem Docker)
```

## Docker

```bash
docker build -t <usuario>/smartkds-producao-service:v1 .
docker push <usuario>/smartkds-producao-service:v1
```

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
