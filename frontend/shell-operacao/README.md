# shell-operacao

**Shell host da operação** — microfrontend do [SmartKDS & Menu](../../README.md) (React 18 + TypeScript + Vite + Module Federation).

Executa o login **OIDC com PKCE** no Keycloak (token apenas em memória) ou o modo demonstração, mantém o `ContextoDeSessao` (token, tenant, papéis) e carrega, por rota e por papel, os remotes `kds/TelaProducao`, `cardapio/TelaCardapio` e `gestao/TelaGestao` (Figura 6), cada um isolado por um limite de erro.

## Regras de microfrontend (verificadas em teste)

- O shell só importa os módulos expostos pelos remotes e injeta a sessão por props.
- Os remotes ficam isolados por limite de erro: a queda de um não derruba os demais.
- O design system (tokens e classes base) é fornecido pelo shell.

## Rodar

```bash
npm install
npm run dev                      # http://localhost:5200 (os remotes precisam estar em preview)
npm test
```

Variáveis (definidas no build): `VITE_API_URL`, `VITE_URL_MESA`, `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_CLIENT_ID`, `VITE_REMOTE_KDS`, `VITE_REMOTE_CARDAPIO`, `VITE_REMOTE_GESTAO`.

## Publicação

Artefato estático publicado em CDN (Azure Static Web Apps). Para o docker-compose existe a imagem nginx genérica:
`docker build --build-arg PROJETO=shell-operacao -t <usuario>/smartkds-shell-operacao:v1 .` (na pasta `frontend/`).

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
