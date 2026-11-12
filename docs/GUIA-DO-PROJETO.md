# SmartKDS & Menu: guia do projeto

Este guia explica como o SmartKDS & Menu funciona, peça por peça, e, principalmente, **quais princípios de arquitetura ele aplica e onde cada um aparece no código**. Ele complementa o documento arc42 (`docs/arc42-entrega3.html`) e o `README.md` da raiz.

---

## Sumário

1. [O problema e a solução](#1-o-problema-e-a-solução)
2. [Visão geral da arquitetura](#2-visão-geral-da-arquitetura)
3. [O caminho de um pedido, do QR Code ao prato entregue](#3-o-caminho-de-um-pedido-do-qr-code-ao-prato-entregue)
4. [Microfrontends](#4-microfrontends)
5. [API Gateway](#5-api-gateway)
6. [BFF (Backend for Frontend)](#6-bff-backend-for-frontend)
7. [Microsserviços](#7-microsserviços)
8. [Vertical Slice + Clean Architecture](#8-vertical-slice--clean-architecture)
9. [Bancos de dados e Database per Service](#9-bancos-de-dados-e-database-per-service)
10. [Multi-tenancy e Row Level Security](#10-multi-tenancy-e-row-level-security)
11. [Mensageria: eventos, Outbox, Inbox, DLQ e Saga](#11-mensageria-eventos-outbox-inbox-dlq-e-saga)
12. [Azure Function](#12-azure-function)
13. [Resiliência](#13-resiliência)
14. [Segurança](#14-segurança)
15. [Testes](#15-testes)
16. [Docker, nuvem e execução local](#16-docker-nuvem-e-execução-local)
17. [Mapa de requisitos da entrega → onde está no projeto](#17-mapa-de-requisitos-da-entrega--onde-está-no-projeto)
18. [Glossário](#18-glossário)

---

## 1. O problema e a solução

Num restaurante tradicional, o garçom anota o pedido, leva até a cozinha e o papel se perde, atrasa ou é lido errado. O **SmartKDS & Menu** resolve isso com duas partes:

- **Menu digital (lado do cliente):** o cliente escaneia o QR Code da mesa, vê o cardápio no celular, monta a comanda e confirma o pedido, sem precisar de login.
- **KDS (Kitchen Display System, lado da operação):** cada estação da cozinha (grill, bar, sobremesas…) recebe na tela, em tempo real, só os itens que lhe cabem. O cozinheiro avança o status (recebido → em preparo → pronto) e dá o *bump* quando o prato sai.

A gestão (gerente/administrador) cadastra cardápio, mesas, estações e regras de roteamento, acompanha indicadores e fecha a conta.

O sistema é **multi-tenant**: vários restaurantes usam a mesma instalação, e os dados de um nunca aparecem para outro.

---

## 2. Visão geral da arquitetura

```
             Navegador do cliente                  Navegador da equipe
         ┌──────────────────────────┐     ┌──────────────────────────────────────┐
         │ shell-mesa  (host)       │     │ shell-operacao (host)                │
         │   └─ mfe-comanda (remote)│     │   ├─ mfe-kds       (remote)          │
         └────────────┬─────────────┘     │   ├─ mfe-cardapio  (remote)          │
                      │                   │   └─ mfe-gestao    (remote)          │
                      │                   └───────────────┬──────────────────────┘
                      │  HTTPS / SSE                       │  HTTPS / SSE + JWT
                      ▼                                    ▼
              ┌──────────────────────────────────────────────────────┐
              │            API Gateway (Spring Cloud Gateway)        │
              │  roteamento · JWT · rate limit · correlation-id      │
              └───────┬───────────────────┬──────────────────┬───────┘
                      ▼                   ▼                  ▼
                 bff-mesa             bff-kds            bff-admin
                 (Node/Fastify)       (Node/Fastify)     (Node/Fastify)  ── GET /aggregated-data
                      │                   │                  │
     ┌────────────────┼───────────────────┼──────────────────┼───────────────────┐
     ▼                ▼                   ▼                  ▼                   ▼
 catalogo-service  pedido-service   producao-service  estabelecimento-service  Azure Function
 (MS1: MongoDB)    (MS2: Azure SQL) (PostgreSQL)      (PostgreSQL)             status-operacional
                        │                 ▲
                        │  RabbitMQ       │
                        └──── eventos ────┘  (pedido.confirmado, ticket.*, pedido.item.cancelado)
                                 │
                                 └──► BFFs consomem eventos e empurram para a tela via SSE
```

| Camada | Tecnologia | Pasta |
|---|---|---|
| Microfrontends | React 18 + Vite 5 + Module Federation | `frontend/` |
| API Gateway | Spring Cloud Gateway (Java 21) | `gateway/api-gateway` |
| BFFs | Node 22, Fastify 5, TypeScript | `bff/bff-mesa`, `bff/bff-kds`, `bff/bff-admin` |
| Microsserviços | Java 21, Spring Boot 3.5 | `services/*-service` |
| Função serverless | Azure Functions v4 (Node/TypeScript) | `functions/status-operacional` |
| Mensageria | RabbitMQ (AMQP 0-9-1) | contratos em `contratos/asyncapi` |
| Identidade | Keycloak (OpenID Connect + PKCE) | `infra/` |

**Regra de ouro das dependências:** cada camada só conversa com a camada imediatamente abaixo. O frontend só fala com o gateway, o gateway só encaminha para os BFFs, e os BFFs são os únicos que chamam os microsserviços e a Function. Nenhum frontend chama um microsserviço direto.

---

## 3. O caminho de um pedido, do QR Code ao prato entregue

Este fluxo é exatamente o que o script `scripts/teste-ponta-a-ponta.mjs` verifica automaticamente (14 verificações).

1. **Cliente lê o QR Code.** A URL é `/m/<qrToken>`. O `shell-mesa` carrega o remote `mfe-comanda`.
2. **Abrir a tela da mesa.** `mfe-comanda` chama `GET /bff/mesa/<qrToken>/cardapio`. O gateway encaminha ao `bff-mesa`, que:
   - pergunta ao `estabelecimento-service` qual mesa/restaurante é dono daquele token (`resolverqrtoken`);
   - busca no `catalogo-service` a versão publicada do cardápio;
   - busca no `pedido-service` a comanda aberta da mesa;
   - junta tudo num único JSON pronto para a tela (**composição de API**).
3. **Montar a comanda.** Cada item adicionado vira `POST /comanda/itens` → `pedido-service` (fatia `adicionaritem`). O preço é **recalculado no servidor** a partir do catálogo, nunca confiado ao navegador.
4. **Confirmar o pedido.** `POST /comanda/confirmacao` com uma `chaveIdempotencia`. Na fatia `confirmarpedido`, o agregado `Comanda` muda os itens de PENDENTE para CONFIRMADO e, **na mesma transação**, grava o evento `PedidoConfirmado` na tabela *outbox*. Se o cliente apertar o botão duas vezes, a segunda chamada devolve o mesmo resultado sem duplicar nada (**idempotência**).
5. **O evento sai.** Um publicador lê a outbox e manda `pedido.confirmado` para o exchange `smartkds.pedidos` no RabbitMQ.
6. **A produção gera tickets.** O `producao-service` consome da fila `producao.pedidos` (fatia `gerartickets`), consulta as regras de roteamento no `estabelecimento-service` e cria um ticket por estação (hambúrguer vai para o *grill*, cerveja vai para o *bar*). Publica `ticket.criado`.
7. **O KDS mostra na hora.** O `bff-kds` também escuta os eventos e envia, via **SSE (Server-Sent Events)**, o ticket só para as telas daquela estação daquele restaurante. No teste local isso leva cerca de 0,4 s.
8. **A cozinha trabalha.** O cozinheiro avança o status (`PATCH /tickets/:id/status`). O `producao-service` publica `ticket.status`; o `pedido-service` consome e marca o item como EM_PRODUCAO. Por isso o cliente **não consegue mais cancelar** esse item (recebe 409 Conflict).
9. **Bump.** `POST /tickets/:id/bump` expede o ticket → evento `ticket.expedido` → `pedido-service` marca o item como ENTREGUE.
10. **Cancelamento (saga).** Se um item ainda cancelável for cancelado, o `pedido-service` publica `pedido.item.cancelado` e o `producao-service` remove o ticket correspondente da tela da estação.
11. **Fechar a conta.** A gestão chama `POST /comandas/:id/fechamento` com a taxa de serviço; o domínio calcula o total (no teste: R$ 105,60).
12. **Indicadores.** `GET /bff/admin/aggregated-data` junta catálogo, pedidos, produção e a Azure Function num painel único.

---

## 4. Microfrontends

### Princípio
**Microfrontend** é aplicar a ideia de microsserviços ao frontend: em vez de um único SPA gigante, a interface é dividida em aplicações menores, cada uma com seu próprio build, deploy e dono, e montadas em tempo de execução.

### Como funciona aqui
Usamos **Module Federation** (`@originjs/vite-plugin-federation`):

- **Hosts (shells):** `shell-mesa` e `shell-operacao`. Cuidam do layout, das rotas, do login e de carregar os remotes. Não têm regra de negócio.
- **Remotes:** `mfe-comanda` (cliente), `mfe-kds` (cozinha), `mfe-cardapio` (CRUD do cardápio) e `mfe-gestao` (painel, comandas, mesas, estações, auditoria). Cada um publica um `remoteEntry.js` que o shell baixa e executa.

Cada remote tem um único arquivo que faz HTTP (`src/api/*Client.ts`), e esse arquivo **só conhece o gateway/BFF**. Exemplo: `frontend/mfe-gestao/src/api/bffGestaoClient.ts` chama apenas `/bff/admin/...`.

### Por que assim
- Equipes diferentes podem publicar o KDS sem tocar no cardápio.
- Uma falha num remote não derruba o shell inteiro.
- O cliente da mesa baixa só o que precisa (não carrega código da gestão).

Todos os frontends usam o mesmo `frontend/Dockerfile` (build com Vite, servido por nginx).

---

## 5. API Gateway

### Princípio
O **API Gateway** é a **porta de entrada única** do sistema. Ele resolve questões transversais (*cross-cutting concerns*) num só lugar, para que BFFs e serviços não repitam essa lógica.

### Como funciona aqui (`gateway/api-gateway`)
- **Roteamento:** `/bff/mesa/**`, `/bff/kds/**` e `/bff/admin/**` vão para o BFF correspondente (o filtro `StripPrefix=2` remove o prefixo). Rotas `/docs/<serviço>/...` expõem apenas o Swagger dos microsserviços, nunca as APIs deles.
- **Autenticação:** valida o JWT emitido pelo Keycloak (`issuer-uri`/`jwk-set-uri`). Rotas da mesa são públicas (a credencial é o próprio `qrToken`); rotas de KDS e admin exigem token. Para SSE, onde o navegador não manda cabeçalho, o token pode vir em `?access_token=`.
- **Contexto (`FiltroDeContexto`):** extrai do JWT o tenant, o usuário e os papéis e repassa como cabeçalhos (`x-tenant-id`, `x-user-roles`…), além de gerar o `x-correlation-id` para rastrear a requisição ponta a ponta.
- **Rate limit (`LimitadorDeTaxa`):** limita requisições por cliente para proteger o sistema de abuso.
- **Proxies confiáveis (`trusted-proxies`):** faz os serviços enxergarem o caminho original, para que o Swagger funcione atrás do gateway.

---

## 6. BFF (Backend for Frontend)

### Princípio
**BFF** é um backend feito **sob medida para um tipo de frontend**. Em vez de uma API genérica que obriga a tela a fazer 5 chamadas e juntar os dados, o BFF entrega exatamente o formato que a tela precisa (*view model*).

### Como funciona aqui
Há **um BFF por experiência de usuário**:

| BFF | Atende | Faz |
|---|---|---|
| `bff-mesa` | cliente na mesa | tela da mesa (mesa + cardápio + comanda), adicionar/cancelar item, confirmar, SSE do status da comanda |
| `bff-kds` | cozinha | fila de tickets por estação com destaque de atraso, avançar status, bump, SSE de tickets |
| `bff-admin` | gestão | **`GET /aggregated-data`**, CRUD do cardápio, comandas, mesas, estações, roteamento, auditoria, visualizador de eventos (`/eventos`) |

Padrões usados dentro dos BFFs:

- **Composição/agregação:** `GET /aggregated-data` chama em paralelo o catálogo (MS1), os pedidos (MS2), a produção e a Azure Function, e devolve tudo junto, com uma lista `fontes` mostrando a situação e a latência de cada uma. Se uma fonte cair, ela aparece como `indisponivel` e o resto continua funcionando (**degradação graciosa**).
- **View model:** o `bff-kds` calcula o destaque de cada ticket (`normal` / `atencao` / `atrasado`) com base no tempo decorrido e no percentual de atenção da estação. A tela só pinta.
- **Tradução de erros:** erros viram **Problem Details (RFC 7807)** (`application/problem+json`), com status e mensagem legíveis.
- **Tempo real:** os BFFs consomem eventos do RabbitMQ e os empurram para os navegadores via SSE, filtrando por tenant e estação.
- **Swagger próprio:** cada BFF publica `/openapi.json` e `/docs`.

---

## 7. Microsserviços

### Princípio
Cada **microsserviço** é dono de **uma capacidade de negócio** (*bounded context* do DDD), tem **seu próprio banco** e pode ser publicado de forma independente. Eles conversam por HTTP quando precisam de uma resposta imediata e por eventos quando basta avisar que algo aconteceu.

| Serviço | Contexto | Banco | Fatias (funcionalidades) |
|---|---|---|---|
| `catalogo-service` (**MS1**) | cardápio, itens, modificadores, versões publicadas | **MongoDB** | cadastraritem, listaritens, obteritem, atualizaritem, alterardisponibilidade, removeritem, publicarversaocardapio, consultarcardapioativo, consultarversaocardapio |
| `pedido-service` (**MS2**) | comandas, itens, confirmação, fechamento, auditoria | **Azure SQL** | abrircomanda, adicionaritem, alteraritemdacomanda, cancelaritem, cancelarcomanda, confirmarpedido, consultarcomanda, fecharcomanda, consultarauditoria + consumidores de eventos |
| `producao-service` | tickets do KDS por estação, SLA, indicadores | PostgreSQL | gerartickets, listarticketsdaestacao, avancarstatusticket, expedirticket, cancelarticketsdoitem, consultarindicadores |
| `estabelecimento-service` | restaurante, mesas, QR Codes, estações, regras de roteamento | PostgreSQL | cadastrarmesa, listarmesas, rotacionarqrtoken, resolverqrtoken, cadastrarestacao, listarestacoes, configurarroteamento, … |

Todos expõem **Swagger/OpenAPI** (springdoc) em `/swagger-ui.html`, acessível pelo gateway em `/docs/<serviço>/swagger-ui.html`.

**Por que o catálogo usa MongoDB?** Um item de cardápio é um documento naturalmente aninhado (grupos de modificadores → opções), e uma versão publicada é um retrato imutável do cardápio inteiro. Isso combina com um banco de documentos.

**Por que pedidos usam Azure SQL?** Comandas exigem transações ACID (dinheiro, status, idempotência), consistência forte e as garantias de travamento usadas pela outbox. Um banco relacional gerenciado na Azure atende a isso.

---

## 8. Vertical Slice + Clean Architecture

Este é o princípio central de organização do código dos microsserviços.

### 8.1 Clean Architecture (o que se quer garantir)
A ideia de Robert C. Martin: **as regras de negócio ficam no centro e não dependem de nada externo**. Banco, framework web e mensageria são detalhes que dependem do domínio, nunca o contrário (**Regra da Dependência**). As quatro responsabilidades clássicas:

| Camada clássica | Responsabilidade | Onde está neste projeto |
|---|---|---|
| **Domain** | entidades, agregados, regras invariantes | `shared/dominio` (ex.: `Comanda`, `ItemDaComanda`, `ComandaId`). **Java puro**: sem Spring, sem JPA. |
| **Application** | casos de uso: orquestra o domínio | o `Handler`/`Query` de cada fatia (ex.: `ConfirmarPedidoHandler`) + `Command`, `Validator` |
| **Infrastructure** | banco, mensageria, clientes HTTP | `shared/persistencia`, `shared/outbox`, `shared/inbox`, `shared/integracao`, `shared/tenancy` |
| **API** | entrada HTTP, tradução para DTO | o `Endpoint` de cada fatia + `shared/web` (Problem Details) |

### 8.2 Vertical Slice (como o código é cortado)
Em vez de pastas por camada técnica (`controllers/`, `services/`, `repositories/`), que espalham uma funcionalidade por vários lugares, o código é organizado **por funcionalidade**. Cada pasta em `features/` é uma **fatia vertical** que atravessa API → aplicação → (usa) domínio/infra.

Exemplo real, `services/pedido-service/.../features/confirmarpedido/`:

```
ConfirmarPedidoEndpoint.java        ← API: recebe o HTTP, monta o Command
ConfirmarPedidoRequest.java         ← API: corpo da requisição
ConfirmarPedidoCommand.java         ← Application: intenção do caso de uso
ConfirmarPedidoValidator.java       ← Application: validação de entrada
ConfirmarPedidoHandler.java         ← Application: orquestra (carrega, chama domínio, salva, publica)
ConfirmarPedidoResponse.java        ← API: resposta
ItemConfirmadoDto.java
PedidoConfirmadoEventFactory.java   ← monta o evento de integração
```

O Handler **não decide regra de negócio**, quem decide é o agregado:

```java
Comanda comanda = repositorio.buscarPorId(tenantId, comandaId).orElseThrow(...);
ResultadoConfirmacao resultado = comanda.confirmarItensPendentes(chaveIdempotencia, agora); // domínio decide
if (!resultado.repeticao()) {
    repositorio.salvar(comanda);
    publicador.publicar(PedidoConfirmadoEventFactory.de(...)); // outbox, mesma transação
}
```

### 8.3 Como as duas ideias se encaixam
- **Vertical Slice** define o **eixo de organização** (por funcionalidade): adicionar uma feature = criar uma pasta, sem mexer nas outras.
- **Clean Architecture** define a **direção das dependências** dentro e fora das fatias: as fatias dependem de `shared/dominio`, o domínio não depende de ninguém, e a infraestrutura fica em `shared/` atrás de interfaces (ex.: `ComandaRepository`, `PublicadorDeEventos`).
- O que é comum (domínio, persistência, outbox) fica em `shared/`; o que é específico de um caso de uso fica na fatia.

### 8.4 As regras são verificadas por teste (ArchUnit)
`src/test/java/.../arquitetura/ArquiteturaVerticalSliceTest.java` em cada serviço quebra o build se alguém violar a arquitetura:

| Regra | O que garante |
|---|---|
| `fatias_nao_dependem_umas_das_outras` | uma feature não importa classes de outra (baixo acoplamento) |
| `fatias_sao_livres_de_ciclos` | não há dependência circular |
| `nao_existem_pacotes_de_camada_tecnica` | proíbe pacotes `controller`/`service`/`repository` genéricos |
| `shared_nao_depende_de_features` | o núcleo compartilhado não conhece as fatias |
| `dominio_e_java_puro` | o domínio só depende de `java..` (Regra da Dependência) |
| `eventos_sao_contratos_puros` | eventos não carregam dependências de framework |
| `toda_fatia_possui_exatamente_um_handler_ou_query` | uma fatia = um caso de uso |
| `classes_da_fatia_nao_sao_publicas` | encapsulamento: a fatia só expõe o endpoint |
| `endpoints_vivem_dentro_de_uma_fatia`, `listeners_vivem_dentro_de_uma_fatia` | toda entrada (HTTP ou fila) pertence a uma fatia |
| `listeners_nao_acessam_repositorios_diretamente` | consumidor de fila delega ao handler |

### 8.5 E nos BFFs (TypeScript)?
A mesma separação aparece em menor escala: `clientes/` (infraestrutura HTTP com resiliência), `composicao/` (montagem de view models: aplicação), `eventos/` e `sse/` (tempo real) e `servidor.ts`/rotas (API).

---

## 9. Bancos de dados e Database per Service

### Princípio
**Database per Service:** cada microsserviço é o **único** que lê e escreve no seu banco. Outro serviço que precise do dado pede via API ou recebe por evento. Isso evita acoplamento pelo esquema do banco.

### Aplicação
- MongoDB (catálogo), Azure SQL (pedidos), PostgreSQL (produção, estabelecimento): **persistência poliglota**, ou seja, cada um escolhe o banco que combina com seus dados.
- **Flyway** versiona o esquema relacional (`db/migration/{vendor}`). O `pedido-service` tem migrações para **SQL Server/Azure SQL** (`UNIQUEIDENTIFIER`, `DATETIMEOFFSET`, `NVARCHAR(MAX)` + `ISJSON`) e uma versão para PostgreSQL usada no modo local. O mesmo código Java roda nos dois, porque o vendor é detectado em tempo de execução.
- Quando a produção precisa do nome do item, ele já vem **dentro do evento** `PedidoConfirmado`, então a produção não consulta o banco de pedidos.

---

## 10. Multi-tenancy e Row Level Security

### Princípio
**Multi-tenancy:** uma única instalação serve vários clientes (restaurantes), isolando os dados de cada um.

### Aplicação em três camadas de defesa
1. **Gateway:** o tenant vem do JWT (claim do Keycloak), e o navegador não consegue forjá-lo. Na mesa, o tenant é resolvido pelo `qrToken`.
2. **Aplicação:** todo repositório filtra por `tenantId` (ex.: `buscarPorId(tenantId, id)`).
3. **Banco (Row Level Security):** mesmo que um bug esqueça o filtro, o banco bloqueia.
   - **Azure SQL:** a cada conexão, `DataSourceComTenant` executa `sp_set_session_context('tenant_id', …)`. A migração V2 cria a função `seguranca.fn_mesmo_tenant` e uma `SECURITY POLICY` que só deixa ver/gravar linhas daquele tenant.
   - **PostgreSQL:** mesma ideia, com `set_config('app.tenant_id', …)` e `CREATE POLICY`.

Nos eventos e no SSE, o tenant também viaja junto, e o `bff-kds` só entrega um ticket para telas do mesmo tenant e estação (há teste que prova isso).

---

## 11. Mensageria: eventos, Outbox, Inbox, DLQ e Saga

### 11.1 Comunicação assíncrona orientada a eventos
Quando o pedido é confirmado, o `pedido-service` **não chama** a produção. Ele **publica um fato** (`pedido.confirmado`), e quem tiver interesse reage. Vantagens: se a produção estiver fora do ar, o pedido é aceito mesmo assim e processado quando ela voltar; novos consumidores entram sem alterar o publicador.

Topologia no RabbitMQ:

| Exchange (topic) | Chave | Fila consumidora |
|---|---|---|
| `smartkds.pedidos` | `pedido.confirmado` | `producao.pedidos` (producao-service) |
| `smartkds.pedidos` | `pedido.item.cancelado` | `producao.cancelamentos` (producao-service) |
| `smartkds.producao` | `ticket.status` | `pedido.producao.status` (pedido-service) |
| `smartkds.producao` | `ticket.expedido` | `pedido.producao` (pedido-service) |
| ambos | `ticket.*`, `pedido.*` | filas exclusivas dos BFFs (para SSE) |

Os contratos estão documentados em **AsyncAPI 3.0** (`contratos/asyncapi`) e podem ser vistos em `/bff/admin/eventos`.

### 11.2 Transactional Outbox (publicar sem perder evento)
**Problema:** salvar no banco e publicar no RabbitMQ são duas operações. Se o banco grava e o broker falha (ou vice-versa), os sistemas ficam inconsistentes.

**Solução:** o evento é gravado numa tabela `outbox` **na mesma transação** do agregado (ou grava tudo, ou nada). Um publicador em segundo plano lê os pendentes, envia ao broker e marca como publicados.

Detalhe importante: com várias instâncias do serviço, duas não podem pegar o mesmo evento. `OutboxSpringData.travarPendentes` usa `@Lock(PESSIMISTIC_WRITE)` com `lock.timeout = -2`, que vira **`SKIP LOCKED`** no PostgreSQL e **`READPAST`** no Azure SQL: cada instância pula as linhas já travadas por outra. Há um teste concorrente (`TravaDaOutbox*Test`) que comprova isso.

A outbox garante entrega **pelo menos uma vez** (*at-least-once*), ou seja, um evento pode chegar duplicado. Por isso existe a inbox.

### 11.3 Inbox (consumidor idempotente)
Cada consumidor registra o `id` do evento numa tabela `inbox` na mesma transação do processamento. Se o mesmo evento chegar de novo, é ignorado. O resultado prático é **exatamente uma vez no efeito**.

### 11.4 DLQ (Dead Letter Queue)
Se uma mensagem falha repetidamente (ex.: corpo inválido), ela não fica travando a fila: vai para `<fila>.dlq` via exchange `smartkds.dlx`. As fatias `registrareventorejeitado` registram essas rejeições para análise.

### 11.5 Saga coreografada
Uma operação que atravessa serviços (cancelar um item) não usa transação distribuída. Cada serviço faz sua parte e publica o próximo evento:

```
pedido-service: cancela item (só se ainda não estiver em produção) ─► pedido.item.cancelado
producao-service: remove o ticket da estação ──────────────────────► ticket.status / SSE
bff-kds: tira o ticket da tela
```

É **coreografia** (sem orquestrador central): cada participante reage a eventos. A regra que protege a consistência fica no domínio: se o item já está EM_PRODUCAO, o cancelamento é recusado com **409 Conflict**.

---

## 12. Azure Function

### Princípio
**Serverless / FaaS:** código executado sob demanda, sem servidor para administrar, cobrado por execução e com escala automática.

### Aplicação (`functions/status-operacional`)
Uma **Function com HTTP trigger** que responde à pergunta *"o restaurante está em horário de pico agora?"*. A partir do fuso e da hora local, devolve:

- `faixaHoraria` (ex.: `madrugada`, `cafe-da-manha`…);
- `emHorarioDePico`;
- `multiplicadorTempoPreparo`;
- uma mensagem.

O `bff-admin` usa isso no `/aggregated-data` para estimar o tempo de preparo considerando o pico (`estimativa.tempoEstimadoComPicoSegundos`). A regra fica em `src/features/status-operacional/calcularStatusOperacional.ts` (também em fatia vertical) e o gatilho HTTP em `src/functions/statusOperacional.ts`. Ela é **sem estado** e independente do resto, o que a torna um ótimo candidato a serverless. Localmente, `servidor-local.mjs` executa o mesmo handler na porta 7071.

---

## 13. Resiliência

| Padrão | Onde | Como |
|---|---|---|
| **Timeout** | clientes HTTP dos BFFs | toda chamada tem tempo máximo |
| **Retry** | `ClienteHttpResiliente` | só em leituras (GET), que são seguras para repetir |
| **Circuit Breaker** | `ClienteHttpResiliente` | após N falhas seguidas, para de chamar a dependência por um tempo e responde 503 na hora (protege quem chama e quem está caído). Erros de negócio (409, 404) **não** abrem o circuito. |
| **Degradação graciosa** | `/aggregated-data`, KDS | fonte indisponível vira `indisponivel`; o KDS usa 70% como percentual de atenção padrão se a configuração da estação cair |
| **Cache** | `CacheEmMemoria` nos BFFs | configurações pouco mutáveis (estações) não são buscadas a cada requisição |
| **Mensageria assíncrona + DLQ** | serviços | falhas temporárias não perdem pedidos |
| **Rate limiting** | gateway | protege contra excesso de requisições |

---

## 14. Segurança

- **OIDC + PKCE com Keycloak:** a equipe faz login no `shell-operacao`; o navegador recebe um JWT; o gateway valida assinatura, emissor e expiração.
- **Papéis (RBAC):** `COZINHA`, `ATENDENTE`, `GERENTE`, `ADMIN`. Ex.: só `COZINHA` pode dar bump (há teste que verifica 403 para `ATENDENTE`).
- **Mesa sem login (privacidade/LGPD):** o cliente não fornece dados pessoais; a credencial é o `qrToken`, que a gestão pode **rotacionar** (`rotacionarqrtoken`) para invalidar QR Codes antigos.
- **Preço calculado no servidor:** o navegador nunca define valores.
- **Isolamento por tenant:** em três camadas (seção 10).
- **Auditoria:** ações da gestão sobre comandas ficam registradas (`consultarauditoria`).
- **Erros padronizados (Problem Details):** não vazam *stack trace*.

---

## 15. Testes

| Tipo | Ferramenta | Exemplos |
|---|---|---|
| **Unitários de domínio** | JUnit 5 | regras do agregado `Comanda` (confirmar, cancelar, fechar, idempotência) |
| **Unitários de handler** | JUnit + Mockito | `CancelarComandaHandlerTest`, `CrudDaComandaTest` |
| **Arquitetura** | **ArchUnit** | `ArquiteturaVerticalSliceTest` (seção 8.4) |
| **Integração de persistência** | zonky (Postgres embutido) e **Testcontainers** (Azure SQL / SQL Server) | `PersistenciaContratoTest` é um contrato abstrato executado contra os dois bancos, garantindo o mesmo comportamento |
| **Concorrência da outbox** | idem | `TravaDaOutbox*Test` (SKIP LOCKED / READPAST) |
| **BFFs** | Vitest | composição, Problem Details, SSE por tenant/estação, circuit breaker, regressões (ex.: bump com corpo vazio) |
| **Function** | Vitest | faixas horárias e pico |
| **Ponta a ponta** | `scripts/teste-ponta-a-ponta.mjs` | fluxo completo da seção 3 passando pelo gateway |

> Os testes contra Azure SQL usam Docker (Testcontainers) e são pulados automaticamente em máquinas sem Docker; no CI eles rodam.

---

## 16. Docker, nuvem e execução local

### Containers
Cada projeto tem seu `Dockerfile` (multi-stage: compila numa imagem e roda numa imagem enxuta). `scripts/publicar-imagens.sh` constrói e publica tudo no **Docker Hub**. O `docker-compose.yml` sobe o ambiente completo (SQL Server 2022 para pedidos, MongoDB, PostgreSQL, RabbitMQ, Keycloak e todos os componentes).

### Nuvem (Azure)
`infra/azure/implantar.sh` cria os recursos: Azure SQL (pedidos), Azure Functions (status operacional) e Container Apps para gateway, BFFs e serviços, com MongoDB Atlas para o catálogo. As URLs públicas vão para o arc42 e para o vídeo.

### Local sem Docker
Para máquinas sem Docker existe `infra/sem-docker`, que sobe PostgreSQL e MongoDB embutidos e o broker AMQP Qpid. Nesse modo, o `pedido-service` usa PostgreSQL (o código é o mesmo, só muda o vendor).

```bash
# 1) infraestrutura (deixe esta janela aberta)
cd infra/sem-docker && java -Djava.protocol.handler.pkgs=org.apache.qpid.server.util.urlstreamhandler \
     -cp "target/classes;$(cat target/classpath.txt)" br.com.smartkds.infra.InfraSemDocker
# 2) aplicações
scripts/rodar-local.sh
# 3) dados de demonstração e teste completo
scripts/popular-demo.sh
node scripts/teste-ponta-a-ponta.mjs
# parar
scripts/parar-local.sh
```

| URL local | O quê |
|---|---|
| http://localhost:5100/m/demo-mesa-1 | mesa do cliente |
| http://localhost:5200 | operação (KDS, cardápio, gestão) |
| http://localhost:8080/bff/admin/aggregated-data | dados agregados |
| http://localhost:8080/bff/admin/docs | Swagger do BFF (troque `admin` por `mesa`/`kds`) |
| http://localhost:8080/docs/catalogo/swagger-ui.html | Swagger do MS1 (troque `catalogo` por `pedido`, `producao` ou `estabelecimento`) |
| http://localhost:8080/bff/admin/eventos | eventos (AsyncAPI) |

---

## 17. Mapa de requisitos da entrega → onde está no projeto

| Requisito | Onde |
|---|---|
| Microfrontend React consumindo só o BFF | `frontend/` (clients em `src/api/` apontam só para `/bff/...`) |
| BFF Node com `GET /aggregated-data` agregando MS1, MS2 e Function | `bff/bff-admin` |
| MS1 com MongoDB + CRUD + Swagger | `services/catalogo-service` |
| MS2 com Azure SQL + CRUD + Swagger | `services/pedido-service` (migrações `db/migration/sqlserver`) |
| Azure Function (HTTP trigger) | `functions/status-operacional` |
| API Gateway | `gateway/api-gateway` |
| Clean Architecture + Vertical Slice | `features/` + `shared/` em cada serviço |
| Testes unitários e de arquitetura | `src/test/...`, `ArquiteturaVerticalSliceTest`, `bff/*/test` |
| Docker Hub | Dockerfiles + `scripts/publicar-imagens.sh` |
| Repositórios GitHub públicos | `scripts/publicar-github.sh` |
| README por projeto com nomes dos alunos | `README.md` em cada pasta |
| arc42 com links, C4, Canvas, prints | `docs/arc42-entrega3.html`, `docs/ENTREGA.md` |

---

## 18. Glossário

- **Agregado:** conjunto de objetos de domínio tratado como unidade de consistência (ex.: `Comanda` + seus itens). Toda mudança passa pela raiz.
- **Bounded Context:** fronteira onde um modelo de domínio tem significado único; aqui, cada microsserviço é um.
- **Bump:** ação do cozinheiro de marcar o ticket como expedido.
- **Correlation ID:** identificador que acompanha uma requisição por todos os componentes, usado para rastrear logs.
- **DLQ:** fila para mensagens que falharam repetidamente.
- **Idempotência:** repetir a mesma operação produz o mesmo resultado, sem efeitos duplicados.
- **KDS:** tela da cozinha que substitui a comanda de papel.
- **Module Federation:** mecanismo que permite carregar código de outra aplicação em tempo de execução (base dos microfrontends).
- **Outbox/Inbox:** tabelas que tornam a publicação e o consumo de eventos confiáveis.
- **Problem Details (RFC 7807):** formato JSON padrão para erros HTTP.
- **RLS (Row Level Security):** filtro de linhas aplicado pelo próprio banco.
- **Saga:** sequência de transações locais coordenadas por eventos, com compensação em vez de transação distribuída.
- **SSE (Server-Sent Events):** canal HTTP em que o servidor empurra eventos para o navegador.
- **Tenant:** cada restaurante cliente do sistema.
- **Vertical Slice:** organização do código por funcionalidade, não por camada técnica.
