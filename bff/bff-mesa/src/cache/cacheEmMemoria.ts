function ehRespostaDefinitiva(erro: unknown): boolean {
  const status = (erro as { status?: unknown } | null)?.status;
  return typeof status === 'number' && status >= 400 && status < 500;
}

/** Cache em memoria com TTL; guarda tambem o ultimo valor para degradacao controlada. */
export class CacheEmMemoria<T> {
  private readonly entradas = new Map<string, { valor: T; expiraEm: number }>();

  constructor(
    private readonly ttlMs: number,
    private readonly relogio: () => number = Date.now,
  ) {}

  async obter(chave: string, carregar: () => Promise<T>): Promise<T> {
    const atual = this.entradas.get(chave);
    if (atual && atual.expiraEm > this.relogio()) return atual.valor;
    try {
      const valor = await carregar();
      this.entradas.set(chave, { valor, expiraEm: this.relogio() + this.ttlMs });
      return valor;
    } catch (erro) {
      // Resposta definitiva da dependencia (4xx, ex.: QR Code rotacionado -> 404): o valor guardado
      // deixou de valer e nao pode ser servido. So a indisponibilidade (5xx/timeout) usa o valor expirado.
      if (ehRespostaDefinitiva(erro)) {
        this.entradas.delete(chave);
        throw erro;
      }
      if (atual) return atual.valor; // dependencia fora do ar: valor expirado e melhor que nenhum
      throw erro;
    }
  }

  invalidar(chave: string): void {
    this.entradas.delete(chave);
  }
}
