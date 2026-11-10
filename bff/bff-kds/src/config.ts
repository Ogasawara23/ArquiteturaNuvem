/** Configuracao por variavel de ambiente (12-factor). */
export const config = {
  porta: Number(process.env.PORT ?? 3002),
  producaoUrl: process.env.PRODUCAO_URL ?? 'http://localhost:8083',
  estabelecimentoUrl: process.env.ESTABELECIMENTO_URL ?? 'http://localhost:8084',
  rabbitmqUrl: process.env.RABBITMQ_URL ?? '',
  /** Prefixo publico no API Gateway, usado no "servers" do OpenAPI (ex.: /bff/kds). */
  prefixoPublico: process.env.PREFIXO_PUBLICO ?? '/bff/kds',
  ttlCacheEstacaoMs: Number(process.env.TTL_CACHE_ESTACAO_MS ?? 60_000),
};
