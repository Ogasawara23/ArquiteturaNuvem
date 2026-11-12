#!/usr/bin/env bash
# Sobe o SmartKDS & Menu localmente SEM Docker (desenvolvimento / demonstracao).
# Pre-requisitos: Java 21+, Maven, Node 22+ e a infraestrutura local ja rodando:
#   cd infra/sem-docker && mvn -q compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
#   java -Djava.protocol.handler.pkgs=org.apache.qpid.server.util.urlstreamhandler \
#        -cp "target/classes;$(cat target/classpath.txt)" br.com.smartkds.infra.InfraSemDocker   (no Linux/macOS use ":" no classpath)
# Neste modo o pedido-service usa PostgreSQL (no docker-compose e na nuvem ele usa Azure SQL).
#   ./scripts/rodar-local.sh            (logs em .logs/, parar com ./scripts/parar-local.sh)
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
LOGS="$RAIZ/.logs"
mkdir -p "$LOGS"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
AMQP="amqp://guest:guest@localhost:5672"

esperar() { # esperar <porta> <nome>
  for _ in $(seq 1 120); do
    if (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; then echo "  ok  $2 (:$1)"; return 0; fi
    sleep 1
  done
  echo "  FALHOU $2 (:$1) - veja $LOGS"; return 1
}

java_app() { # java_app <pasta> <nome> [VAR=valor ...]
  local pasta=$1 nome=$2; shift 2
  [[ -f "$RAIZ/$pasta/target/app.jar" ]] || (cd "$RAIZ/$pasta" && mvn -q -B package -DskipTests)
  ( cd "$RAIZ/$pasta" && env "$@" "$JAVA" -XX:TieredStopAtLevel=1 -jar target/app.jar > "$LOGS/$nome.log" 2>&1 & )
}

node_app() { # node_app <pasta> <nome> <comando> [VAR=valor ...]
  local pasta=$1 nome=$2 cmd=$3; shift 3
  ( cd "$RAIZ/$pasta" && [[ -d node_modules ]] || npm install --no-audit --no-fund > /dev/null )
  ( cd "$RAIZ/$pasta" && env "$@" bash -c "$cmd" > "$LOGS/$nome.log" 2>&1 & )
}

echo ">> microsservicos"
java_app services/estabelecimento-service estabelecimento-service RABBITMQ_URL=$AMQP
java_app services/catalogo-service catalogo-service
java_app services/pedido-service pedido-service DB_URL=jdbc:postgresql://localhost:5432/smartkds_pedido \
  DB_USER=pedido DB_PASSWORD=pedido RABBITMQ_URL=$AMQP SMARTKDS_DLQ=false
java_app services/producao-service producao-service RABBITMQ_URL=$AMQP SMARTKDS_DLQ=false
java_app gateway/api-gateway api-gateway

echo ">> Azure Function (host local) e BFFs"
node_app functions/status-operacional status-operacional "npm run build > /dev/null && node servidor-local.mjs"
node_app bff/bff-mesa bff-mesa "npm run build > /dev/null && node dist/index.js" RABBITMQ_URL=$AMQP
node_app bff/bff-kds bff-kds "npm run build > /dev/null && node dist/index.js" RABBITMQ_URL=$AMQP
node_app bff/bff-admin bff-admin "npm run build > /dev/null && node dist/index.js"

echo ">> microfrontends (remotes primeiro, depois shells)"
for p in mfe-comanda:5101 mfe-kds:5201 mfe-cardapio:5202 mfe-gestao:5203 shell-mesa:5100 shell-operacao:5200; do
  nome=${p%%:*}; porta=${p##*:}
  node_app "frontend/$nome" "$nome" "npx vite build > /dev/null && npx vite preview --host 127.0.0.1 --port $porta --strictPort"
done

echo ">> aguardando"
esperar 8084 estabelecimento-service; esperar 8081 catalogo-service; esperar 8082 pedido-service
esperar 8083 producao-service; esperar 8080 api-gateway; esperar 7071 status-operacional
esperar 3001 bff-mesa; esperar 3002 bff-kds; esperar 3003 bff-admin
for p in 5101 5201 5202 5203 5100 5200; do esperar $p "frontend :$p"; done

cat <<EOF

SmartKDS & Menu no ar (modo local sem Docker)
  Operacao (gerente/cozinha/atendente) ... http://localhost:5200
  Mesa do cliente (QR Code Mesa 1) ....... http://localhost:5100/m/demo-mesa-1
  API Gateway ............................ http://localhost:8080
  GET /aggregated-data ................... http://localhost:8080/bff/admin/aggregated-data
  Swagger BFF admin / mesa / kds ......... http://localhost:8080/bff/admin/docs  (/bff/mesa/docs, /bff/kds/docs)
  Swagger microsservicos ................. http://localhost:8080/docs/catalogo/swagger-ui.html  (pedido, producao, estabelecimento)
  Eventos (AsyncAPI) ..................... http://localhost:8080/bff/admin/eventos
Primeira vez? Popule o cardapio: ./scripts/popular-demo.sh
EOF
