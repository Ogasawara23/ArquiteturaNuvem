#!/usr/bin/env bash
# Roteiro de implantacao na nuvem (Azure for Students) a partir das imagens do Docker Hub.
#
# Servicos gerenciados usados (todos com plano gratuito ou de credito estudantil):
#   - MongoDB Atlas M0 (Free Tier)               -> catalogo-service           (MONGODB_URI)
#   - Azure SQL Database (oferta gratuita / Basic) -> pedido-service (Microsservico 2), criado por este script
#   - PostgreSQL (Azure Flexible B1ms ou Neon free)  -> producao, estabelecimento (rode infra/postgres/init.sql)
#   - CloudAMQP "Little Lemur" (RabbitMQ gratuito) -> RABBITMQ_URL (amqps://...)
#   - Azure Container Apps (consumo; MIN_REPLICAS) -> 4 microsservicos, 3 BFFs, API Gateway
#   - Azure Functions (Consumption, Node 22)       -> status-operacional (HTTP Trigger)
#   - Azure Static Web Apps (Free)                 -> 2 shells + 4 remotes (CDN)
#
# Pre-requisitos: az login; imagens publicadas com scripts/publicar-imagens.sh; variaveis abaixo exportadas.
set -euo pipefail

: "${DOCKERHUB_USER:?}" "${MONGODB_URI:?}" "${RABBITMQ_URL:?}"
: "${PG_HOST:?ex.: smartkds-pg.postgres.database.azure.com}"
: "${PG_SENHA_PRODUCAO:?}" "${PG_SENHA_ESTABELECIMENTO:?}"
: "${SQL_ADMIN_SENHA:?senha forte do administrador do Azure SQL}"
TAG="${TAG:-v1}"
# Replica minima 1: os consumidores do RabbitMQ (pedido, producao e BFFs) precisam estar de pe para
# receber eventos, e isso evita a partida a frio do Java durante o video. Depois use MIN_REPLICAS=0.
MIN_REPLICAS="${MIN_REPLICAS:-1}"
RG="${RG:-rg-smartkds}"
LOC="${LOC:-brazilsouth}"
# Regiao da Function: nem toda regiao tem plano de consumo (ex.: chilecentral) -> use LOC_FUNCAO=canadacentral
LOC_FUNCAO="${LOC_FUNCAO:-$LOC}"
AMBIENTE="${AMBIENTE:-cae-smartkds}"
FUNCAO="${FUNCAO:-smartkds-status-$RANDOM}"
STORAGE="${STORAGE:-stsmartkds$RANDOM}"
SQL_SERVIDOR="${SQL_SERVIDOR:-sql-smartkds-$RANDOM}"
SQL_ADMIN="${SQL_ADMIN:-smartkdsadmin}"

az group create -n "$RG" -l "$LOC" -o none
az extension add --name containerapp --upgrade -y -o none
# Reexecucao segura: recursos que ja existem nao sao recriados (passe SQL_SERVIDOR/FUNCAO existentes)
existe() { "$@" -o none >/dev/null 2>&1; }
# Modo WorkloadProfiles (perfil Consumption): o modo Express, padrao em CLIs recentes, nao isola o ingresso
# interno (servicos ficam publicos) e nao resolve o nome curto dos apps (http://catalogo-service).
existe az containerapp env show -n "$AMBIENTE" -g "$RG" || \
  az containerapp env create -n "$AMBIENTE" -g "$RG" -l "$LOC" --environment-mode WorkloadProfiles -o none

# app <nome> <porta> <internal|external> VAR=valor ...
app() {
  local nome=$1 porta=$2 ingresso=$3; shift 3
  az containerapp create -n "$nome" -g "$RG" --environment "$AMBIENTE" \
    --image "docker.io/$DOCKERHUB_USER/smartkds-$nome:$TAG" \
    --target-port "$porta" --ingress "$ingresso" \
    --min-replicas "$MIN_REPLICAS" --max-replicas 2 --cpu 0.5 --memory 1.0Gi \
    --env-vars "$@" -o none
  echo "  $nome implantado ($ingresso)"
}

jdbc() { echo "jdbc:postgresql://$PG_HOST:5432/$1?sslmode=require"; }

echo ">> Azure SQL Database (Microsservico 2 - pedido-service)"
existe az sql server show -n "$SQL_SERVIDOR" -g "$RG" || \
  az sql server create -n "$SQL_SERVIDOR" -g "$RG" -l "$LOC" -u "$SQL_ADMIN" -p "$SQL_ADMIN_SENHA" -o none
az sql server firewall-rule create -g "$RG" -s "$SQL_SERVIDOR" -n servicos-azure   --start-ip-address 0.0.0.0 --end-ip-address 0.0.0.0 -o none
# Oferta gratuita (serverless, 100k vCore-s/mes). Sem ela, use: --edition Basic --capacity 5
existe az sql db show -g "$RG" -s "$SQL_SERVIDOR" -n smartkds_pedido || az sql db create -g "$RG" -s "$SQL_SERVIDOR" -n smartkds_pedido --use-free-limit --free-limit-exhaustion-behavior AutoPause   --edition GeneralPurpose --family Gen5 --capacity 2 --compute-model Serverless -o none   || az sql db create -g "$RG" -s "$SQL_SERVIDOR" -n smartkds_pedido --edition Basic --capacity 5 -o none
URL_SQL="jdbc:sqlserver://$SQL_SERVIDOR.database.windows.net:1433;databaseName=smartkds_pedido;encrypt=true;trustServerCertificate=false;loginTimeout=30"

echo ">> Azure Function (HTTP Trigger)"
existe az functionapp show -n "$FUNCAO" -g "$RG" || {
  az storage account create -n "$STORAGE" -g "$RG" -l "$LOC_FUNCAO" --sku Standard_LRS -o none
  az functionapp create -n "$FUNCAO" -g "$RG" --storage-account "$STORAGE" --consumption-plan-location "$LOC_FUNCAO" \
    --runtime node --runtime-version 22 --functions-version 4 --os-type Linux -o none
}
( cd "$(dirname "$0")/../../functions/status-operacional" && npm ci && npm run build && func azure functionapp publish "$FUNCAO" --typescript )
URL_FUNCAO="https://$(az functionapp show -n "$FUNCAO" -g "$RG" --query defaultHostName -o tsv)/api/status-operacional"

echo ">> Microsservicos (ingresso interno: somente BFFs e gateway os alcancam)"
app estabelecimento-service 8084 internal DB_URL="$(jdbc smartkds_estabelecimento)" DB_USER=estabelecimento DB_PASSWORD="$PG_SENHA_ESTABELECIMENTO"
app catalogo-service 8081 internal MONGODB_URI="$MONGODB_URI"
app pedido-service 8082 internal DB_URL="$URL_SQL" DB_USER="$SQL_ADMIN" DB_PASSWORD="$SQL_ADMIN_SENHA" \
  RABBITMQ_URL="$RABBITMQ_URL" CATALOGO_URL=http://catalogo-service
app producao-service 8083 internal DB_URL="$(jdbc smartkds_producao)" DB_USER=producao DB_PASSWORD="$PG_SENHA_PRODUCAO" \
  RABBITMQ_URL="$RABBITMQ_URL" ESTABELECIMENTO_URL=http://estabelecimento-service

echo ">> BFFs (ingresso interno: atras do API Gateway)"
app bff-mesa 3001 internal CATALOGO_URL=http://catalogo-service PEDIDO_URL=http://pedido-service \
  ESTABELECIMENTO_URL=http://estabelecimento-service RABBITMQ_URL="$RABBITMQ_URL"
app bff-kds 3002 internal PRODUCAO_URL=http://producao-service ESTABELECIMENTO_URL=http://estabelecimento-service \
  RABBITMQ_URL="$RABBITMQ_URL"
app bff-admin 3003 internal CATALOGO_URL=http://catalogo-service PEDIDO_URL=http://pedido-service \
  PRODUCAO_URL=http://producao-service ESTABELECIMENTO_URL=http://estabelecimento-service FUNCTION_STATUS_URL="$URL_FUNCAO"

echo ">> API Gateway (unica porta publica)"
app api-gateway 8080 external BFF_MESA_URL=http://bff-mesa BFF_KDS_URL=http://bff-kds BFF_ADMIN_URL=http://bff-admin \
  CATALOGO_URL=http://catalogo-service PEDIDO_URL=http://pedido-service PRODUCAO_URL=http://producao-service \
  ESTABELECIMENTO_URL=http://estabelecimento-service JWT_HABILITADO="${JWT_HABILITADO:-false}" \
  ${KEYCLOAK_ISSUER_URI:+KEYCLOAK_ISSUER_URI="$KEYCLOAK_ISSUER_URI"} ${KEYCLOAK_JWKS_URI:+KEYCLOAK_JWKS_URI="$KEYCLOAK_JWKS_URI"}
# Variavel vazia sobrescreve o padrao do application.yml (jwkSetUri cannot be empty): so passe se definida
URL_GATEWAY="https://$(az containerapp show -n api-gateway -g "$RG" --query properties.configuration.ingress.fqdn -o tsv)"

cat <<EOF

Backend implantado.
  API Gateway ........ $URL_GATEWAY
  Swagger BFFs ....... $URL_GATEWAY/bff/mesa/docs  $URL_GATEWAY/bff/kds/docs  $URL_GATEWAY/bff/admin/docs
  Swagger servicos ... $URL_GATEWAY/docs/catalogo/swagger-ui.html (idem pedido, producao, estabelecimento)
  Eventos (AsyncAPI) . $URL_GATEWAY/bff/admin/eventos
  aggregated-data .... $URL_GATEWAY/bff/admin/aggregated-data
  Azure Function ..... $URL_FUNCAO

Proximos passos:
  1. API=$URL_GATEWAY ./scripts/popular-demo.sh
  2. Frontends no Azure Static Web Apps (remotes primeiro, depois os shells):
       URL_GATEWAY=$URL_GATEWAY RG=$RG ./infra/azure/implantar-frontends.sh
  3. Teste ponta a ponta na nuvem:
       API=$URL_GATEWAY QR=demo-mesa-4 node scripts/teste-ponta-a-ponta.mjs
EOF
