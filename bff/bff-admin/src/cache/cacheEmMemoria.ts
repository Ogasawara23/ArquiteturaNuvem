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
      if (atual) return atual.valor; // valor expirado e melhor que nenhum
      throw erro;
    }
  }

  invalidar(chave: string): void {
    this.entradas.delete(chave);
  }
}
