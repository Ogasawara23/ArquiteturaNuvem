# shell-mesa

**Shell host público do cliente** — microfrontend do [SmartKDS & Menu](../../README.md) (React 18 + TypeScript + Vite + Module Federation).

Host mínimo aberto pela URL do QR Code (`/m/{qrToken}`). Não autentica: o qrToken é a credencial de escopo limitado da mesa. Carrega o remote `comanda/TelaComanda` com limite de erro (se o remote falhar, aparece um aviso amigável).

## Regras de microfrontend (verificadas em teste)

- O shell só importa os módulos expostos pelos remotes e injeta a sessão por props.
- Os remotes ficam isolados por limite de erro: a queda de um não derruba os demais.
- O design system (tokens e classes base) é fornecido pelo shell.

## Rodar

```bash
npm install
npm run dev                      # http://localhost:5100 (os remotes precisam estar em preview)
npm test
```

Variáveis (definidas no build): `VITE_API_URL` (API Gateway), `VITE_REMOTE_COMANDA` (URL do `remoteEntry.js` do mfe-comanda).

## Publicação

Artefato estático publicado em CDN (Azure Static Web Apps). Para o docker-compose existe a imagem nginx genérica:
`docker build --build-arg PROJETO=shell-mesa -t <usuario>/smartkds-shell-mesa:v1 .` (na pasta `frontend/`).

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
