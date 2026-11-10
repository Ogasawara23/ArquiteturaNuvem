/** Configuracao por variavel de ambiente (12-factor). */
export const config = {
  porta: Number(process.env.PORT ?? 3003),
  catalogoUrl: process.env.CATALOGO_URL ?? 'http://localhost:8081',
  pedidoUrl: process.env.PEDIDO_URL ?? 'http://localhost:8082',
  producaoUrl: process.env.PRODUCAO_URL ?? 'http://localhost:8083',
  estabelecimentoUrl: process.env.ESTABELECIMENTO_URL ?? 'http://localhost:8084',
  /** Azure Function (HTTP Trigger), ex.: https://smartkds-func.azurewebsites.net/api/status-operacional */
  functionStatusUrl: process.env.FUNCTION_STATUS_URL ?? 'http://localhost:7071/api/status-operacional',
  functionKey: process.env.FUNCTION_KEY ?? '',
  prefixoPublico: process.env.PREFIXO_PUBLICO ?? '/bff/admin',
  arquivoAsyncApi: process.env.ASYNCAPI_ARQUIVO ?? 'contratos/smartkds-eventos.yaml',
};
