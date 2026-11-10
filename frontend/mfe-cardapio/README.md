# mfe-cardapio

**Remote: edição e publicação do cardápio** — microfrontend do [SmartKDS & Menu](../../README.md) (React 18 + TypeScript + Vite + Module Federation).

Expõe `./TelaCardapio`. CRUD de itens com editor compacto de modificadores, alternância disponível/esgotado e publicação de nova versão imutável.

## Regras de microfrontend (verificadas em teste)

- Nenhum import de outro remote: a composição acontece no shell.
- O remote não lê token do localStorage/sessionStorage: a sessão vem do shell.
- Componentes não chamam `fetch` direto: toda chamada passa por `src/api` e vai apenas ao BFF, via API Gateway.

## Rodar

```bash
npm install
npm run build && npm run preview  # http://localhost:5202/assets/remoteEntry.js (remotes do Module Federation rodam a partir do build)
npm test
```

Variáveis (definidas no build): `VITE_API_URL` (somente no modo standalone).

## Publicação

Artefato estático publicado em CDN (Azure Static Web Apps). Para o docker-compose existe a imagem nginx genérica:
`docker build --build-arg PROJETO=mfe-cardapio -t <usuario>/smartkds-mfe-cardapio:v1 .` (na pasta `frontend/`).

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
