# mfe-comanda

**Remote: cardápio e comanda do cliente** — microfrontend do [SmartKDS & Menu](../../README.md) (React 18 + TypeScript + Vite + Module Federation).

Expõe `./TelaComanda`. Cardápio por categoria, modal de modificadores (com validação de obrigatórios e máximo), carrinho, envio à cozinha com **chave de idempotência** (toque duplo não duplica o pedido), cancelamento enquanto permitido e acompanhamento em tempo real por SSE.

## Regras de microfrontend (verificadas em teste)

- Nenhum import de outro remote: a composição acontece no shell.
- O remote não lê token do localStorage/sessionStorage: a sessão vem do shell.
- Componentes não chamam `fetch` direto: toda chamada passa por `src/api` e vai apenas ao BFF, via API Gateway.

## Rodar

```bash
npm install
npm run build && npm run preview  # http://localhost:5101/assets/remoteEntry.js (remotes do Module Federation rodam a partir do build)
npm test
```

Variáveis (definidas no build): `VITE_API_URL` (somente no modo standalone).

## Publicação

Artefato estático publicado em CDN (Azure Static Web Apps). Para o docker-compose existe a imagem nginx genérica:
`docker build --build-arg PROJETO=mfe-comanda -t <usuario>/smartkds-mfe-comanda:v1 .` (na pasta `frontend/`).

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
