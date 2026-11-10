import { app, HttpRequest, HttpResponseInit, InvocationContext } from '@azure/functions';
import { calcularStatusOperacional, FusoInvalidoError } from '../features/status-operacional/calcularStatusOperacional';

/**
 * Camada de entrada (HTTP Trigger). Apenas traduz HTTP <-> funcao pura da feature.
 * GET /api/status-operacional?tenantId=demo&fuso=America/Sao_Paulo
 */
export async function statusOperacional(request: HttpRequest, context: InvocationContext): Promise<HttpResponseInit> {
  const tenantId = request.query.get('tenantId') ?? 'desconhecido';
  const fuso = request.query.get('fuso') ?? process.env.FUSO_PADRAO ?? 'America/Sao_Paulo';
  const correlationId = request.headers.get('x-correlation-id') ?? context.invocationId;
  try {
    const status = calcularStatusOperacional(tenantId, fuso, new Date());
    context.log(`status-operacional tenant=${tenantId} pico=${status.emHorarioDePico} correlation=${correlationId}`);
    return { status: 200, jsonBody: status, headers: { 'x-correlation-id': correlationId } };
  } catch (erro) {
    if (erro instanceof FusoInvalidoError) {
      return {
        status: 400,
        jsonBody: { title: 'Requisicao invalida', status: 400, detail: erro.message, correlationId },
        headers: { 'content-type': 'application/problem+json' },
      };
    }
    throw erro;
  }
}

app.http('status-operacional', {
  methods: ['GET'],
  authLevel: 'anonymous',
  route: 'status-operacional',
  handler: statusOperacional,
});
