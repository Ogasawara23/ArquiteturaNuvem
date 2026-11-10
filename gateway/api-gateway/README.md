# api-gateway

**API Gateway** do [SmartKDS & Menu](../../README.md) (Spring Cloud Gateway, ADR-02): a única porta de entrada.
Nenhum navegador fala diretamente com um BFF ou microsserviço.

## Responsabilidades (somente borda, sem regra de negócio)

- **Roteamento** por prefixo com `StripPrefix=2`: `/bff/mesa/**` → bff-mesa, `/bff/kds/**` → bff-kds, `/bff/admin/**` → bff-admin.
- **JWT do Keycloak** (`JWT_HABILITADO=true`): assinatura via JWKS, emissor e expiração; `realm_access.roles` → papéis.
  `/bff/mesa/**` é público (a credencial é o qrToken). Para SSE, aceita `?access_token=` (EventSource não envia cabeçalhos).
- **Isolamento multi-tenant**: sempre **sobrescreve** `X-Tenant-Id`, `X-User-Id` e `X-User-Roles` a partir do token (o cliente não forja).
- **Rate limit** por tenant (rotas autenticadas) e por IP (rotas públicas de mesa), com resposta 429 em Problem Details.
- **CORS**, geração/propagação de `X-Correlation-Id`.
- **Documentação**: `/docs/<servico>/swagger-ui.html` expõe apenas o Swagger dos microsserviços (as rotas `/interno/**` continuam inacessíveis).

Modo demonstração (`JWT_HABILITADO=false`, padrão): aceita `X-Tenant-Id` do cliente ou usa `TENANT_DEMO=demo`.

## Variáveis de ambiente

| Variável | Padrão |
|---|---|
| `BFF_MESA_URL` / `BFF_KDS_URL` / `BFF_ADMIN_URL` | `http://localhost:3001` / `3002` / `3003` |
| `CATALOGO_URL`, `PEDIDO_URL`, `PRODUCAO_URL`, `ESTABELECIMENTO_URL` | portas 8081 a 8084 (apenas Swagger) |
| `JWT_HABILITADO` | `false` |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/smartkds` |
| `KEYCLOAK_JWKS_URI` | `.../protocol/openid-connect/certs` |
| `CORS_ORIGINS` | `*` |
| `LIMITE_TENANT_RPS` / `LIMITE_MESA_RPS` | `50` / `10` |

## Rodar e testar

```bash
mvn spring-boot:run      # porta 8080
mvn test                 # filtros de borda + ArchUnit (gateway sem controllers, sem banco, sem composição)
docker build -t <usuario>/smartkds-api-gateway:v1 . && docker push <usuario>/smartkds-api-gateway:v1
```

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
