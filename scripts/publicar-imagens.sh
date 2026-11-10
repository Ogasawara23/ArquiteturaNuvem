#!/usr/bin/env bash
# Publica no Docker Hub as imagens dos microsservicos, BFFs, API Gateway e Azure Function.
#   DOCKERHUB_USER=seuusuario TAG=v1 ./scripts/publicar-imagens.sh
# Padrao exigido: docker build -t <registry>/<repo>/<imagem>:<tag> . && docker push ...
# No Docker Hub o "registry/repo" e o proprio usuario: docker.io/<usuario>/smartkds-<imagem>:<tag>
set -euo pipefail

: "${DOCKERHUB_USER:?defina DOCKERHUB_USER}"
TAG="${TAG:-v1}"
REGISTRY="${REGISTRY:-docker.io}"
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"

declare -A IMAGENS=(
  [catalogo-service]="services/catalogo-service"
  [pedido-service]="services/pedido-service"
  [producao-service]="services/producao-service"
  [estabelecimento-service]="services/estabelecimento-service"
  [api-gateway]="gateway/api-gateway"
  [bff-mesa]="bff/bff-mesa"
  [bff-kds]="bff/bff-kds"
  [bff-admin]="bff/bff-admin"
  [status-operacional-function]="functions/status-operacional"
)

docker login "$REGISTRY"
for nome in "${!IMAGENS[@]}"; do
  imagem="$REGISTRY/$DOCKERHUB_USER/smartkds-$nome:$TAG"
  echo ">>> $imagem"
  docker build -t "$imagem" "$RAIZ/${IMAGENS[$nome]}"
  docker push "$imagem"
done

# Microfrontends (opcional: em producao vao para CDN/Static Web Apps)
if [[ "${INCLUIR_FRONTEND:-false}" == "true" ]]; then
  for projeto in shell-mesa mfe-comanda shell-operacao mfe-kds mfe-cardapio mfe-gestao; do
    imagem="$REGISTRY/$DOCKERHUB_USER/smartkds-$projeto:$TAG"
    docker build --build-arg PROJETO="$projeto" \
      --build-arg VITE_API_URL="${VITE_API_URL:-http://localhost:8080}" -t "$imagem" "$RAIZ/frontend"
    docker push "$imagem"
  done
fi
echo "Imagens publicadas em https://hub.docker.com/u/$DOCKERHUB_USER"
