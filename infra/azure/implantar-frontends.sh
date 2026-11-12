#!/usr/bin/env bash
# Publica os 6 microfrontends no Azure Static Web Apps (plano Free), um site por projeto.
# Ordem obrigatoria: os 4 remotes primeiro (os shells precisam das URLs dos remoteEntry.js no build).
#
# Uso: URL_GATEWAY=https://api-gateway.<...>.azurecontainerapps.io ./infra/azure/implantar-frontends.sh
# Pre-requisitos: az login; Node 22; o backend ja implantado por implantar.sh.
set -euo pipefail

: "${URL_GATEWAY:?informe a URL publica do API Gateway (saida do implantar.sh)}"
RG="${RG:-rg-smartkds}"
LOC_SWA="${LOC_SWA:-eastus2}"   # Static Web Apps nao existe em brazilsouth
SUFIXO="${SUFIXO:-$RANDOM}"
RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"

criar_site() {   # criar_site <projeto> -> imprime a URL https do site
  local nome="swa-smartkds-$1-$SUFIXO"
  az staticwebapp create -n "$nome" -g "$RG" -l "$LOC_SWA" --sku Free -o none
  echo "https://$(az staticwebapp show -n "$nome" -g "$RG" --query defaultHostname -o tsv)"
}

publicar() {     # publicar <projeto>  (variaveis VITE_* ja exportadas)
  local nome="swa-smartkds-$1-$SUFIXO"
  local token; token="$(az staticwebapp secrets list -n "$nome" -g "$RG" --query properties.apiKey -o tsv)"
  ( cd "$RAIZ/frontend/$1" && npm ci && npm run build && \
    npx --yes @azure/static-web-apps-cli deploy dist --deployment-token "$token" --env production )
  echo "  $1 publicado"
}

export VITE_API_URL="$URL_GATEWAY"

echo ">> Remotes"
declare -A URL
for r in mfe-comanda mfe-kds mfe-cardapio mfe-gestao; do
  URL[$r]="$(criar_site "$r")"
  publicar "$r"
done

echo ">> Shells"
URL[shell-mesa]="$(criar_site shell-mesa)"
URL[shell-operacao]="$(criar_site shell-operacao)"

VITE_REMOTE_COMANDA="${URL[mfe-comanda]}/assets/remoteEntry.js" publicar shell-mesa
VITE_REMOTE_KDS="${URL[mfe-kds]}/assets/remoteEntry.js" \
VITE_REMOTE_CARDAPIO="${URL[mfe-cardapio]}/assets/remoteEntry.js" \
VITE_REMOTE_GESTAO="${URL[mfe-gestao]}/assets/remoteEntry.js" \
VITE_URL_MESA="${URL[shell-mesa]}" \
  publicar shell-operacao

cat <<EOF

Frontends publicados.
  Operacao (KDS, cardapio, gestao) ... ${URL[shell-operacao]}
  Mesa do cliente ..................... ${URL[shell-mesa]}/m/demo-mesa-1   (apos rodar popular-demo.sh)
  Remotes: ${URL[mfe-comanda]}  ${URL[mfe-kds]}  ${URL[mfe-cardapio]}  ${URL[mfe-gestao]}

Opcional (restringir CORS do gateway aos frontends):
  az containerapp update -n api-gateway -g $RG --set-env-vars CORS_ORIGINS="${URL[shell-mesa]},${URL[shell-operacao]}"
EOF
