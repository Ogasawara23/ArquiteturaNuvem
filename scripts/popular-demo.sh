#!/usr/bin/env bash
# Popula o cardapio do tenant "demo" pelo API Gateway (bff-admin) e publica a primeira versao.
# As mesas (demo-mesa-1..4, demo-balcao-1), estacoes e regras ja vem da migracao V2 do estabelecimento-service.
#   API=https://seu-gateway ./scripts/popular-demo.sh        (padrao: http://localhost:8080)
#   TOKEN=<jwt-do-keycloak>                                    (obrigatorio se o gateway estiver com JWT_HABILITADO=true)
set -euo pipefail
API="${API:-http://localhost:8080}"
CAB=(-H "content-type: application/json" -H "X-Tenant-Id: demo")
[[ -n "${TOKEN:-}" ]] && CAB+=(-H "Authorization: Bearer $TOKEN")

item() { curl -fsS -X POST "$API/bff/admin/cardapio/itens" "${CAB[@]}" -d "$1" > /dev/null && echo "  + $(echo "$1" | grep -o '"nome":"[^"]*"' | head -1 | cut -d'"' -f4)"; }

echo "Cadastrando itens em $API ..."
item '{"nome":"Hamburguer Artesanal 180g","descricao":"Pao brioche, cheddar e cebola caramelizada","categoria":"Lanches","precoCentavos":4200,"estacaoSugerida":"grill","tempoAlvoPreparoSegundos":720,"gruposModificadores":[{"nome":"Ponto da carne","obrigatorio":true,"minimoSelecoes":1,"maximoSelecoes":1,"opcoes":[{"nome":"Mal passado","precoAdicionalCentavos":0,"disponivel":true},{"nome":"Ao ponto","precoAdicionalCentavos":0,"disponivel":true},{"nome":"Bem passado","precoAdicionalCentavos":0,"disponivel":true}]},{"nome":"Adicionais","obrigatorio":false,"minimoSelecoes":0,"maximoSelecoes":3,"opcoes":[{"nome":"Bacon extra","precoAdicionalCentavos":600,"disponivel":true},{"nome":"Cheddar extra","precoAdicionalCentavos":400,"disponivel":true},{"nome":"Ovo","precoAdicionalCentavos":300,"disponivel":true}]}]}'
item '{"nome":"Picanha na Chapa","descricao":"300 g, arroz, farofa e vinagrete","categoria":"Pratos","precoCentavos":8900,"estacaoSugerida":"grill","tempoAlvoPreparoSegundos":900,"gruposModificadores":[{"nome":"Ponto da carne","obrigatorio":true,"minimoSelecoes":1,"maximoSelecoes":1,"opcoes":[{"nome":"Ao ponto","precoAdicionalCentavos":0,"disponivel":true},{"nome":"Bem passado","precoAdicionalCentavos":0,"disponivel":true}]}]}'
item '{"nome":"Batata Frita","descricao":"Porcao de 400 g","categoria":"Porcoes","precoCentavos":2800,"estacaoSugerida":"fritura","tempoAlvoPreparoSegundos":480,"gruposModificadores":[{"nome":"Molho","obrigatorio":false,"minimoSelecoes":0,"maximoSelecoes":2,"opcoes":[{"nome":"Maionese da casa","precoAdicionalCentavos":0,"disponivel":true},{"nome":"Cheddar e bacon","precoAdicionalCentavos":900,"disponivel":true}]}]}'
item '{"nome":"Isca de Frango","descricao":"Empanada, com molho barbecue","categoria":"Porcoes","precoCentavos":3400,"estacaoSugerida":"fritura","tempoAlvoPreparoSegundos":540,"gruposModificadores":[]}'
item '{"nome":"Limonada Suica","categoria":"Bebidas","precoCentavos":1400,"estacaoSugerida":"bar","tempoAlvoPreparoSegundos":180,"gruposModificadores":[{"nome":"Acucar","obrigatorio":true,"minimoSelecoes":1,"maximoSelecoes":1,"opcoes":[{"nome":"Com acucar","precoAdicionalCentavos":0,"disponivel":true},{"nome":"Sem acucar","precoAdicionalCentavos":0,"disponivel":true}]}]}'
item '{"nome":"Refrigerante Lata","categoria":"Bebidas","precoCentavos":700,"estacaoSugerida":"bar","tempoAlvoPreparoSegundos":60,"gruposModificadores":[]}'
item '{"nome":"Petit Gateau","descricao":"Com sorvete de creme","categoria":"Sobremesas","precoCentavos":2600,"estacaoSugerida":"sobremesa","tempoAlvoPreparoSegundos":420,"gruposModificadores":[]}'

echo "Publicando a versao do cardapio..."
curl -fsS -X POST "$API/bff/admin/cardapio/versoes" "${CAB[@]}" -d '{"publicadaPor":"script-demo"}'; echo
echo "Pronto. Mesa de teste: abra o shell-mesa em /m/demo-mesa-1"
