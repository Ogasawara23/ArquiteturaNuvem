# estabelecimento-service

**Estabelecimento — mesas, QR Codes, estações e roteamento (PostgreSQL)** — parte do [SmartKDS & Menu](../../README.md).

Configuração do tenant: mesas e seus `qrToken` (192 bits, rotacionáveis), estações de produção, regras de roteamento item/categoria → estação e resolução do QR Code em `(tenantId, mesaId)`.

A migração `V2` cria o tenant de demonstração `demo` com as mesas `demo-mesa-1` a `demo-mesa-4` e `demo-balcao-1`, as estações grill, fritura, bar, sobremesa e cozinha e as regras Bebidas → bar e Sobremesas → sobremesa.

## Arquitetura do código: Vertical Slice + Clean Architecture

```
br/com/smartkds/estabelecimento/
├── features/            uma fatia por funcionalidade
│   ├── cadastrarestacao/
│   ├── cadastrarmesa/
│   ├── configurarestabelecimento/
│   ├── configurarroteamento/
│   ├── consultarestabelecimento/
│   ├── consultarroteamento/
│   ├── listarestacoes/
│   ├── listarmesas/
│   ├── resolverqrtoken/
│   ├── rotacionarqrtoken/
├── shared/              núcleo compartilhado do contexto
│   ├── dominio/
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
| PUT / GET | `/interno/estabelecimento` | `configurarestabelecimento / consultarestabelecimento` |
| POST | `/interno/mesas` | `cadastrarmesa` |
| GET | `/interno/mesas` | `listarmesas` |
| POST | `/interno/mesas/{id}/qr-token` | `rotacionarqrtoken` |
| GET | `/interno/mesas/por-token/{qrToken}` | `resolverqrtoken (sem X-Tenant-Id)` |
| POST | `/interno/estacoes` | `cadastrarestacao` |
| GET | `/interno/estacoes[/{codigo}]` | `listarestacoes` |
| PUT | `/interno/roteamento` | `configurarroteamento` |
| GET | `/interno/roteamento` | `consultarroteamento` |

Swagger: `http://localhost:8084/swagger-ui.html` (direto) ou `<api-gateway>/docs/estabelecimento/swagger-ui.html` (publicado).

## Tecnologias

Java 21 · Spring Boot 3.5 · Spring Web · Spring Data JPA · Flyway · PostgreSQL 16 (RLS) · springdoc-openapi · JUnit 5 · ArchUnit · PostgreSQL embutido (zonky) nos testes de integração

## Variáveis de ambiente

| Variável | Padrão local | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/smartkds_estabelecimento` |  |
| `DB_USER / DB_PASSWORD` | `estabelecimento / estabelecimento` |  |

## Como rodar localmente

```bash
docker compose up -d postgres   # na raiz do monorepo
mvn spring-boot:run
mvn test   # unitários + ArchUnit + integração com PostgreSQL real embutido (sem Docker)
```

## Docker

```bash
docker build -t <usuario>/smartkds-estabelecimento-service:v1 .
docker push <usuario>/smartkds-estabelecimento-service:v1
```

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
