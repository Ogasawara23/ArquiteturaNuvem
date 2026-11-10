# mfe-kds

**Remote: tela de produção (KDS)** — microfrontend do [SmartKDS & Menu](../../README.md) (React 18 + TypeScript + Vite + Module Federation).

Expõe `./TelaProducao`. Seletor de estação, fila ordenada por urgência com cronômetro local, destaque normal/atenção/atrasado, um botão por transição (Iniciar preparo → Pronto → Bump) e SSE com recarga do snapshot a cada evento ou reconexão.

## Regras de microfrontend (verificadas em teste)

- Nenhum import de outro remote: a composição acontece no shell.
- O remote não lê token do localStorage/sessionStorage: a sessão vem do shell.
- Componentes não chamam `fetch` direto: toda chamada passa por `src/api` e vai apenas ao BFF, via API Gateway.

## Rodar

```bash
npm install
npm run build && npm run preview  # http://localhost:5201/assets/remoteEntry.js (remotes do Module Federation rodam a partir do build)
npm test
```

Variáveis (definidas no build): `VITE_API_URL` (somente no modo standalone).

## Publicação

Artefato estático publicado em CDN (Azure Static Web Apps). Para o docker-compose existe a imagem nginx genérica:
`docker build --build-arg PROJETO=mfe-kds -t <usuario>/smartkds-mfe-kds:v1 .` (na pasta `frontend/`).

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
