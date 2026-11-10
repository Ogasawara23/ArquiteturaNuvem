# status-operacional (Azure Function)

**Azure Function com HTTP Trigger** do [SmartKDS & Menu](../../README.md), consumida pelo `bff-admin` no `GET /aggregated-data`.

`GET /api/status-operacional?tenantId=demo&fuso=America/Sao_Paulo` devolve a faixa horária (café, almoço, jantar...),
se o restaurante está em **horário de pico** e o **multiplicador do tempo de preparo** (1,3 nos picos e 1,5 no almoço de fim de semana),
usado pelo painel do gerente para estimar o tempo de espera.

## Organização

- `src/features/status-operacional/calcularStatusOperacional.ts`: regra pura (sem SDK do Azure), testável isoladamente.
- `src/functions/statusOperacional.ts`: camada de entrada (modelo de programação v4), só traduz HTTP ↔ regra.
- Teste de arquitetura: a feature não pode importar `@azure/functions`.

## Rodar, testar e publicar

```bash
npm install && npm test
cp local.settings.example.json local.settings.json
npm start                                   # requer Azure Functions Core Tools v4: http://localhost:7071/api/status-operacional
az functionapp create ... --runtime node --runtime-version 20 --functions-version 4
func azure functionapp publish <nome-da-function-app>
docker build -t <usuario>/smartkds-status-operacional-function:v1 .   # imagem para o docker-compose
```

## Alunos

Arthur Sales Major · Vinicius Ogasawara · Luiz Eduardo Assi · Rodrigo Petrelli Glir
