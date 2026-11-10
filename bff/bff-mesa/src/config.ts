/** Configuracao por variavel de ambiente (12-factor). */
export const config = {
  porta: Number(process.env.PORT ?? 3001),
  catalogoUrl: process.env.CATALOGO_URL ?? 'http://localhost:8081',
  pedidoUrl: process.env.PEDIDO_URL ?? 'http://localhost:8082',
  estabelecimentoUrl: process.env.ESTABELECIMENTO_URL ?? 'http://localhost:8084',
  rabbitmqUrl: process.env.RABBITMQ_URL ?? '',
  prefixoPublico: process.env.PREFIXO_PUBLICO ?? '/bff/mesa',
  ttlCacheMesaMs: Number(process.env.TTL_CACHE_MESA_MS ?? 60_000),
  ttlCacheCardapioMs: Number(process.env.TTL_CACHE_CARDAPIO_MS ?? 30_000),
};
