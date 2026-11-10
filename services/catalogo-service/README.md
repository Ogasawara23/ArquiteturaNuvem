# catalogo-service

**Microsserviço 1 — Catálogo (MongoDB)** — parte do [SmartKDS & Menu](../../README.md).

Mantém itens do cardápio, grupos de modificadores, tempo-alvo e estação sugerida, e publica **versões imutáveis** do cardápio (ADR-11). É o **Microsserviço 1** da entrega: MongoDB Atlas, CRUD completo e Swagger.

Persistência: coleções `itens`, `versoes_cardapio` (documento fechado com a cópia dos itens) e `cardapio_ativo` (ponteiro da versão ativa por tenant).

## Arquitetura do código: Vertical Slice + Clean Architecture

```
br/com/smartkds/catalogo/
├── features/            uma fatia por funcionalidade
│   ├── alterardisponibilidade/
│   ├── atualizaritem/
│   ├── cadastraritem/
│   ├── consultarcardapioativo/
│   ├── consultarversaocardapio/
│   ├── listaritens/
│   ├── obteritem/
│   ├── publicarversaocardapio/
│   ├── removeritem/
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
| POST | `/interno/itens` | `cadastraritem` |
| GET | `/interno/itens?categoria=` | `listaritens` |
| GET | `/interno/itens/{id}` | `obteritem` |
| PUT | `/interno/itens/{id}` | `atualizaritem` |
| PATCH | `/interno/itens/{id}/disponibilidade` | `alterardisponibilidade` |
| DELETE | `/interno/itens/{id}` | `removeritem` |
| POST | `/interno/cardapios/versoes` | `publicarversaocardapio` |
| GET | `/interno/cardapios/ativo` | `consultarcardapioativo` |
| GET | `/interno/cardapios/{versaoId}[/itens/{itemId}]` | `consultarversaocardapio` |

Swagger: `http://localhost:8081/swagger-ui.html` (direto) ou `<api-gateway>/docs/catalogo/swagger-ui.html` (publicado).

## Tecnologias

Java 21 · Spring Boot 3.5 · Spring Web · Spring Data MongoDB · springdoc-openapi · JUnit 5 · ArchUnit

## Variáveis de ambiente

| Variável | Padrão local | Descrição |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/smartkds_catalogo` | Conexão (Atlas: `mongodb+srv://...`) |
| `PORT` | `8081` | Porta HTTP |

## Como rodar localmente

```bash
docker compose up -d mongodb   # na raiz do monorepo
mvn spring-boot:run
mvn test   # unitários + ArchUnit
```

## Docker

```bash
docker build -t <usuario>/smartkds-catalogo-service:v1 .
docker push <usuario>/smartkds-catalogo-service:v1
```

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
