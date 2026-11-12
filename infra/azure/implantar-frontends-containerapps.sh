#!/usr/bin/env bash
# Alternativa ao implantar-frontends.sh para assinaturas em que o Azure Static Web Apps nao e permitido
# (ex.: Azure for Students restrito a regioes sem SWA). Cada microfrontend vira um Container App
# publico com nginx (frontend/Dockerfile), no mesmo ambiente do backend.
#
# Etapa 1 - gerar a configuracao de build (URLs entram no bundle Vite):
#   ./infra/azure/implantar-frontends-containerapps.sh configurar
#   git add infra/azure/frontend.env && git commit && git tag fe-v1 && git push origin main fe-v1
#   (o workflow cd-frontend.yml publica as imagens smartkds-<projeto>:fe-v1 no Docker Hub)
# Etapa 2 - implantar as imagens:
#   DOCKERHUB_USER=<usuario> TAG=fe-v1 ./infra/azure/implantar-frontends-containerapps.sh implantar
set -euo pipefail

RG="${RG:-rg-smartkds}"
AMBIENTE="${AMBIENTE:-cae-smartkds}"
MIN_REPLICAS="${MIN_REPLICAS:-1}"
RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
PROJETOS=(mfe-comanda mfe-kds mfe-cardapio mfe-gestao shell-mesa shell-operacao)

dominio() { az containerapp env show -n "$AMBIENTE" -g "$RG" --query properties.defaultDomain -o tsv; }

case "${1:-}" in
  configurar)
    DOMINIO="$(dominio)"
    GATEWAY="https://$(az containerapp show -n api-gateway -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)"
    cat > "$RAIZ/infra/azure/frontend.env" <<EOF
# Gerado por implantar-frontends-containerapps.sh (URLs publicas, sem segredos)
URL_GATEWAY=$GATEWAY
DOMINIO_FRONTEND=$DOMINIO
EOF
    echo "infra/azure/frontend.env gerado (gateway $GATEWAY, dominio $DOMINIO)"
    ;;
  implantar)
    : "${DOCKERHUB_USER:?}" "${TAG:?ex.: fe-v1}"
    for p in "${PROJETOS[@]}"; do
      az containerapp create -n "$p" -g "$RG" --environment "$AMBIENTE" \
        --image "docker.io/$DOCKERHUB_USER/smartkds-$p:$TAG" \
        --target-port 80 --ingress external \
        --min-replicas "$MIN_REPLICAS" --max-replicas 1 --cpu 0.25 --memory 0.5Gi -o none
      echo "  $p implantado"
    done
    DOMINIO="$(dominio)"
    cat <<EOF

Frontends publicados.
  Operacao (KDS, cardapio, gestao) ... https://shell-operacao.$DOMINIO
  Mesa do cliente ..................... https://shell-mesa.$DOMINIO/m/demo-mesa-1
EOF
    ;;
  *) echo "uso: $0 configurar|implantar" >&2; exit 2 ;;
esac
