#!/usr/bin/env bash
# Publica cada parte do SmartKDS & Menu em um repositorio PUBLICO proprio no GitHub, como pede a entrega
# (Microfrontend, BFF, Microsservico Mongo, Microsservico SQL, Azure Function), alem dos demais servicos
# e do repositorio "guarda-chuva" com a documentacao, o docker-compose e a infraestrutura.
#
# Pre-requisitos: GitHub CLI autenticado (gh auth login) e git configurado (user.name / user.email).
#   GITHUB_OWNER=<usuario-ou-organizacao> ./scripts/publicar-github.sh
set -euo pipefail

: "${GITHUB_OWNER:?defina GITHUB_OWNER (usuario ou organizacao do GitHub)}"
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
TEMP="$(mktemp -d)"

# repositorio | pasta de origem | tipo de CI
REPOS=(
  "smartkds-microfrontends|frontend|node-multi"
  "smartkds-bff|bff|node-multi"
  "smartkds-catalogo-service|services/catalogo-service|maven"
  "smartkds-pedido-service|services/pedido-service|maven"
  "smartkds-status-operacional-function|functions/status-operacional|node"
  "smartkds-api-gateway|gateway/api-gateway|maven"
  "smartkds-producao-service|services/producao-service|maven"
  "smartkds-estabelecimento-service|services/estabelecimento-service|maven"
  "smartkds-menu|.|monorepo"
)

ci_maven() { cat <<'EOF'
name: ci
on: [push, pull_request]
jobs:
  testes:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5
      - uses: actions/setup-java@v5
        with: { distribution: temurin, java-version: '21', cache: maven }
      - run: mvn -B -ntp verify   # unitarios + ArchUnit + integracao (Testcontainers usa o Docker do runner)
EOF
}

ci_node() { cat <<'EOF'
name: ci
on: [push, pull_request]
jobs:
  testes:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5
      - uses: actions/setup-node@v5
        with: { node-version: '22' }
      - run: npm ci && npm test && npm run build
EOF
}

ci_node_multi() { cat <<EOF
name: ci
on: [push, pull_request]
jobs:
  testes:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        projeto: [$1]
    steps:
      - uses: actions/checkout@v5
      - uses: actions/setup-node@v5
        with: { node-version: '22' }
      - run: npm ci && npm test && npm run build
        working-directory: \${{ matrix.projeto }}
EOF
}

for linha in "${REPOS[@]}"; do
  IFS='|' read -r nome origem tipo <<< "$linha"
  destino="$TEMP/$nome"
  echo ">>> $nome ($origem)"
  mkdir -p "$destino"
  ( cd "$RAIZ/$origem" && tar --exclude=node_modules --exclude=target --exclude=dist --exclude=coverage \
      --exclude=.git --exclude=local.settings.json -cf - . ) | ( cd "$destino" && tar -xf - )
  cp "$RAIZ/.gitignore" "$destino/.gitignore"

  if [[ "$tipo" != "monorepo" ]]; then
    mkdir -p "$destino/.github/workflows"
    case "$tipo" in
      maven) ci_maven > "$destino/.github/workflows/ci.yml" ;;
      node) ci_node > "$destino/.github/workflows/ci.yml" ;;
      node-multi)
        projetos=$(cd "$destino" && ls -d */ | tr -d '/' | grep -v '^nginx$' | paste -sd, -)
        ci_node_multi "$projetos" > "$destino/.github/workflows/ci.yml" ;;
    esac
  fi

  (
    cd "$destino"
    git init -q -b main
    git add -A
    git commit -q -m "SmartKDS & Menu - $nome (Entrega 3 PJBL)"
    if gh repo view "$GITHUB_OWNER/$nome" > /dev/null 2>&1; then
      git remote add origin "https://github.com/$GITHUB_OWNER/$nome.git"
      git push -u origin main --force-with-lease
    else
      gh repo create "$GITHUB_OWNER/$nome" --public --source . --push \
        --description "SmartKDS & Menu - $nome (cardapio digital, comanda eletronica e KDS)"
    fi
  )
  echo "    https://github.com/$GITHUB_OWNER/$nome"
done

rm -rf "$TEMP"
echo "Repositorios publicados. Atualize a tabela de links em docs/ENTREGA.md."
