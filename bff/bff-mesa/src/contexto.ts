import { randomUUID } from 'node:crypto';
import type { FastifyRequest } from 'fastify';

/** Contexto confiavel da requisicao: os cabecalhos sao sempre sobrescritos pelo API Gateway. */
export interface Contexto {
  tenantId: string;
  usuarioId: string;
  papeis: string[];
  correlationId: string;
}

export class ErroHttp extends Error {
  constructor(
    readonly status: number,
    readonly titulo: string,
    detalhe: string,
    readonly corpo?: unknown,
  ) {
    super(detalhe);
  }
}

export function contextoDe(requisicao: FastifyRequest): Contexto {
  const cabecalho = (nome: string) => {
    const valor = requisicao.headers[nome];
    return Array.isArray(valor) ? valor[0] : valor;
  };
  const tenantId = cabecalho('x-tenant-id');
  if (!tenantId) {
    throw new ErroHttp(401, 'Nao autenticado', 'requisicao sem tenant: acesse pelo API Gateway');
  }
  return {
    tenantId,
    usuarioId: cabecalho('x-user-id') ?? 'desconhecido',
    papeis: (cabecalho('x-user-roles') ?? '').split(',').map((p) => p.trim().toUpperCase()).filter(Boolean),
    correlationId: cabecalho('x-correlation-id') ?? randomUUID(),
  };
}

/** Autorizacao fina reafirmada no BFF (ADR-04). */
export function exigirPapel(contexto: Contexto, ...aceitos: string[]): void {
  if (!aceitos.some((p) => contexto.papeis.includes(p))) {
    throw new ErroHttp(403, 'Acesso negado', `operacao exige um dos papeis: ${aceitos.join(', ')}`);
  }
}
