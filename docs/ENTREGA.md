# Entrega 3 — links, publicação e roteiro do vídeo

Este arquivo reúne o que precisa entrar no PDF arc42 da Entrega 3. O conteúdo novo do arc42 (links, Canvas, C4 de contexto, contêineres e componentes, ADRs e quadros de prints) está pronto em [`arc42-entrega3.html`](arc42-entrega3.html): abra no navegador, cole os prints e use *Imprimir → Salvar como PDF*, anexando ao PDF das entregas anteriores. Substitua `<...>` pelos valores reais depois de publicar.

## 1. Links para o PDF (seção nova "Links da entrega", sugerida logo após "Sobre este documento")

| Item | Link |
|---|---|
| GitHub (repositório público, monorepo) | `https://github.com/Ogasawara23/ArquiteturaNuvem` |
| GitHub — Microfrontend / BFF | `https://github.com/Ogasawara23/ArquiteturaNuvem/tree/main/frontend` · `https://github.com/Ogasawara23/ArquiteturaNuvem/tree/main/bff` |
| GitHub — MS Mongo / MS SQL / Function | `https://github.com/Ogasawara23/ArquiteturaNuvem/tree/main/services/catalogo-service` · `https://github.com/Ogasawara23/ArquiteturaNuvem/tree/main/services/pedido-service` · `https://github.com/Ogasawara23/ArquiteturaNuvem/tree/main/functions/status-operacional` |
| GitHub — demais | `https://github.com/Ogasawara23/ArquiteturaNuvem/tree/main/gateway/api-gateway` · `https://github.com/Ogasawara23/ArquiteturaNuvem/tree/main/services/producao-service` · `https://github.com/Ogasawara23/ArquiteturaNuvem/tree/main/services/estabelecimento-service` |
| Docker Hub (imagens) | `https://hub.docker.com/u/<usuario>` — `smartkds-catalogo-service`, `smartkds-pedido-service`, `smartkds-producao-service`, `smartkds-estabelecimento-service`, `smartkds-api-gateway`, `smartkds-bff-mesa`, `smartkds-bff-kds`, `smartkds-bff-admin`, `smartkds-status-operacional-function` |
| API Gateway | `https://<gateway>` |
| Endpoint agregado | `https://<gateway>/bff/admin/aggregated-data` |
| Swagger BFF mesa / KDS / admin | `https://<gateway>/bff/mesa/docs` · `https://<gateway>/bff/kds/docs` · `https://<gateway>/bff/admin/docs` |
| Swagger microsserviços | `https://<gateway>/docs/catalogo/swagger-ui.html` · `/docs/pedido/...` · `/docs/producao/...` · `/docs/estabelecimento/...` |
| Swagger dos eventos (AsyncAPI) | `https://<gateway>/bff/admin/eventos` (YAML em `/bff/admin/asyncapi.yaml`) |
| Azure Function | `https://<function-app>.azurewebsites.net/api/status-operacional?tenantId=demo` |
| Frontend — operação | `https://<swa-shell-operacao>` |
| Frontend — mesa (QR Code) | `https://<swa-shell-mesa>/m/demo-mesa-1` |
| Vídeo no YouTube | `https://youtu.be/<id>` |

## 2. Passo a passo de publicação

1. **GitHub**: o projeto está no repositório público `https://github.com/Ogasawara23/ArquiteturaNuvem` (monorepo, uma pasta por projeto, ADR-17). Opcional: `GITHUB_OWNER=<usuario> ./scripts/publicar-github.sh` cria um repositório por projeto.
2. **Docker Hub**: `DOCKERHUB_USER=<usuario> TAG=v1 ./scripts/publicar-imagens.sh` (ou tag `v1` com os segredos do `cd.yml`).
3. **Dados gerenciados**: cluster M0 no MongoDB Atlas (libere o IP `0.0.0.0/0` ou os IPs de saída do Container Apps); o **Azure SQL Database** é criado pelo `implantar.sh` (oferta gratuita ou Basic 5 DTU); PostgreSQL (Azure Flexible B1ms ou Neon) com `infra/postgres/init.sql` para produção e estabelecimento (troque as senhas); instância gratuita no CloudAMQP.
4. **Backend na Azure**: exporte as variáveis do cabeçalho de `infra/azure/implantar.sh` e execute-o.
5. **Dados de demonstração**: `API=https://<gateway> ./scripts/popular-demo.sh`.
6. **Frontends**: `URL_GATEWAY=<gateway> ./infra/azure/implantar-frontends.sh` publica os 4 remotes no Azure Static Web Apps e depois os 2 shells apontando para os `remoteEntry.js` publicados
   (CORS dos remotes em `public/staticwebapp.config.json`).
7. Preencha a tabela da seção 1 em `arc42-entrega3.html`, capture os 9 prints listados na seção 7 dele e gere o PDF.

## 3. Roteiro do vídeo (sempre com URLs da nuvem, nunca localhost)

1. **Arquitetura (1 min)**: Figura 2 do arc42 e o mapa "requisito → implementação" do README.
2. **Cliente (2 min)**: abrir `https://<swa-shell-mesa>/m/demo-mesa-1` no celular (ou ler o QR Code gerado na aba Mesas),
   montar o pedido com modificadores e enviar à cozinha.
3. **Cozinha (2 min)**: no shell de operação, perfil Cozinha, estação Grill: o ticket aparece **sem recarregar** (SSE);
   Iniciar preparo → Pronto → Bump. Mostrar o item do cliente mudando para "Em preparo" e "Entregue" em tempo real.
4. **Saga de cancelamento (1 min)**: confirmar um item e cancelá-lo antes do preparo; o ticket some do KDS.
5. **Gerente (2 min)**: aba Gestão → Painel consumindo `GET /aggregated-data`; abrir a URL do endpoint no navegador e mostrar as
   quatro fontes (MS1, MS2, Azure Function e produção). Fechar uma comanda e mostrar a auditoria.
6. **Contratos (1 min)**: Swagger de um BFF, Swagger do catálogo (CRUD), AsyncAPI dos eventos, Docker Hub e GitHub Actions verde.
7. **Qualidade (1 min)**: rodar `mvn test` do pedido-service mostrando as regras ArchUnit e o teste de integração com RLS;
   opcionalmente quebrar uma regra (importar o handler de outra fatia) e mostrar o build falhando (Seção 8.8.3).

## 4. Ajustes a refletir no arc42 (implementação × documento)

| Tema | Documento | Implementado | Motivo |
|---|---|---|---|
| Composição dos handlers | `HandlerConfig` cria handlers com visibilidade de pacote | Handlers anotados com `@Component` (continuam package-private) | `HandlerConfig` em outro pacote não compila com handlers package-private |
| Ponteiro da versão ativa | Citado no estabelecimento e no catálogo | Fica só no catálogo (`cardapio_ativo`) | Um único dono; o pedido consulta a versão ativa no catálogo |
| Snapshot na adição do item | Versão gravada na comanda | Versão **ativa no momento da adição** (o preço segue congelado no item) | Itens novos publicados durante a refeição podem ser pedidos |
| Roteamento por categoria | Precedência item → categoria → sugerida → padrão | Igual, com `itemCardapioId` e `categoria` adicionados ao `PedidoConfirmado` | O evento não carregava a categoria |
| Filas de consumo | `pedido.producao` para `ticket.expedido` | `pedido.producao` + `pedido.producao.status`; `producao.pedidos` + `producao.cancelamentos` | Uma fila por tipo de evento consumido (um listener por fila) |
| Filas de SSE | `kds.stream` / `mesa.stream` duráveis | Fila exclusiva e temporária por réplica (`kds.stream.<host>.<pid>`) | Cada réplica precisa receber todos os eventos dos seus clientes SSE |
| Rate limit | Redis (`RequestRateLimiter`) | Token bucket em memória por réplica | Economicidade; Redis registrado como evolução (Seção 11) |
| Swagger dos microsserviços | — | Rotas somente de documentação no gateway (`/docs/<servico>/**`) | Requisito da entrega sem expor `/interno/**` |
| Token no SSE | — | `?access_token=` aceito apenas pelo gateway | `EventSource` não envia cabeçalho `Authorization` |
| Microsserviço 1/2 da entrega | — | MS1 = catalogo-service (MongoDB Atlas), MS2 = pedido-service (**Azure SQL**) | Mapeamento para o enunciado |
| Banco do pedido-service | PostgreSQL | Azure SQL Database (migrações também para PostgreSQL) | Exigência da entrega (ADR-13) |
| CRUD da comanda | sem alteração/exclusão | PATCH item do carrinho e DELETE da comanda (exclusão lógica com compensação) | CRUD completo exigido (ADR-18) |
| Azure Function | Não constava | `status-operacional` (HTTP Trigger) usada no `/aggregated-data` | Requisito da entrega |
| Endpoint agregado | — | `GET /bff/admin/aggregated-data` | Requisito da entrega |
| Equipe | Tabela com 2 integrantes | Capa e READMEs com 4 integrantes | Alinhar a tabela "Integrantes do grupo" à capa |
