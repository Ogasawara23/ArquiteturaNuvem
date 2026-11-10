import type { ServerResponse } from 'node:http';

export interface EventoSse {
  tipo: string;
  dados: unknown;
  id?: string;
}

/**
 * Mantem as conexoes SSE abertas por chave (ex.: "tenant:grill") e reemite os eventos
 * recebidos do broker. O SSE e canal de NOTIFICACAO: a tela sempre recarrega o snapshot
 * ao reconectar (Figura 17), entao perder um evento nunca deixa a tela errada.
 */
export class GerenciadorDeStreams {
  private readonly conexoes = new Map<string, Set<ServerResponse>>();
  private readonly batimento: NodeJS.Timeout;

  constructor(intervaloBatimentoMs = 15_000) {
    this.batimento = setInterval(() => this.enviarATodos(': ping\n\n'), intervaloBatimentoMs);
    this.batimento.unref();
  }

  registrar(chave: string, conexao: ServerResponse): void {
    const conjunto = this.conexoes.get(chave) ?? new Set<ServerResponse>();
    conjunto.add(conexao);
    this.conexoes.set(chave, conjunto);
  }

  encerrar(chave: string, conexao: ServerResponse): void {
    const conjunto = this.conexoes.get(chave);
    conjunto?.delete(conexao);
    if (conjunto && conjunto.size === 0) this.conexoes.delete(chave);
  }

  publicar(chave: string, evento: EventoSse): number {
    const conjunto = this.conexoes.get(chave);
    if (!conjunto) return 0;
    const quadro = formatar(evento);
    for (const conexao of conjunto) conexao.write(quadro);
    return conjunto.size;
  }

  /** Publica para todas as chaves que comecam com o prefixo (ex.: todas as estacoes do tenant). */
  publicarParaPrefixo(prefixo: string, evento: EventoSse): number {
    let total = 0;
    for (const chave of this.conexoes.keys()) {
      if (chave.startsWith(prefixo)) total += this.publicar(chave, evento);
    }
    return total;
  }

  conexoesAtivas(chave?: string): number {
    if (chave) return this.conexoes.get(chave)?.size ?? 0;
    let total = 0;
    for (const c of this.conexoes.values()) total += c.size;
    return total;
  }

  fechar(): void {
    clearInterval(this.batimento);
    for (const conjunto of this.conexoes.values()) for (const c of conjunto) c.end();
    this.conexoes.clear();
  }

  private enviarATodos(texto: string): void {
    for (const conjunto of this.conexoes.values()) for (const c of conjunto) c.write(texto);
  }
}

export function formatar(evento: EventoSse): string {
  const linhas = [`event: ${evento.tipo}`];
  if (evento.id) linhas.push(`id: ${evento.id}`);
  linhas.push(`data: ${JSON.stringify(evento.dados)}`);
  return `${linhas.join('\n')}\n\n`;
}

/** Abre a resposta SSE sobre o objeto cru do Node (sem buffer e sem timeout de inatividade). */
export function abrirStream(resposta: ServerResponse, correlationId: string): void {
  resposta.writeHead(200, {
    'content-type': 'text/event-stream; charset=utf-8',
    'cache-control': 'no-cache, no-transform',
    connection: 'keep-alive',
    'x-accel-buffering': 'no',
    'x-correlation-id': correlationId,
  });
  resposta.write('retry: 3000\n\n');
}
