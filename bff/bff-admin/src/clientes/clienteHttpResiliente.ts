import { ErroHttp, type Contexto } from '../contexto.js';

export interface OpcoesResiliencia {
  timeoutMs: number;
  tentativasLeitura: number;
  limiteFalhasConsecutivas: number;
  tempoCircuitoAbertoMs: number;
}

const PADRAO: OpcoesResiliencia = {
  timeoutMs: 2500,
  tentativasLeitura: 2,
  limiteFalhasConsecutivas: 5,
  tempoCircuitoAbertoMs: 10_000,
};

export interface Requisicao {
  metodo: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  caminho: string;
  contexto?: Contexto;
  corpo?: unknown;
  consulta?: Record<string, string | number | undefined>;
}

/**
 * Cliente HTTP com timeout curto, nova tentativa apenas em leitura (GET e idempotente)
 * e circuit breaker por dependencia (Secao 4.5). Erros 4xx do servico sao repassados
 * como Problem Details; falhas de rede e 5xx viram 503.
 */
export class ClienteHttpResiliente {
  private falhasConsecutivas = 0;
  private abertoAte = 0;
  private readonly opcoes: OpcoesResiliencia;

  constructor(
    readonly nome: string,
    private readonly urlBase: string,
    opcoes: Partial<OpcoesResiliencia> = {},
    private readonly relogio: () => number = Date.now,
  ) {
    this.opcoes = { ...PADRAO, ...opcoes };
  }

  get circuitoAberto(): boolean {
    return this.relogio() < this.abertoAte;
  }

  async requisitar<T>(req: Requisicao): Promise<T> {
    if (this.circuitoAberto) {
      throw new ErroHttp(503, 'Dependencia indisponivel', `${this.nome} indisponivel (circuito aberto)`);
    }
    const tentativas = req.metodo === 'GET' ? this.opcoes.tentativasLeitura : 1;
    let ultimoErro: unknown;
    for (let tentativa = 1; tentativa <= tentativas; tentativa++) {
      try {
        const resultado = await this.executar<T>(req);
        this.falhasConsecutivas = 0;
        return resultado;
      } catch (erro) {
        if (erro instanceof ErroHttp && erro.status < 500) {
          this.falhasConsecutivas = 0;
          throw erro; // erro de negocio: nao conta como falha da dependencia
        }
        ultimoErro = erro;
      }
    }
    this.registrarFalha();
    if (ultimoErro instanceof ErroHttp) throw ultimoErro;
    throw new ErroHttp(503, 'Dependencia indisponivel', `${this.nome} indisponivel`);
  }

  private registrarFalha(): void {
    this.falhasConsecutivas++;
    if (this.falhasConsecutivas >= this.opcoes.limiteFalhasConsecutivas) {
      this.abertoAte = this.relogio() + this.opcoes.tempoCircuitoAbertoMs;
      this.falhasConsecutivas = 0;
    }
  }

  private async executar<T>(req: Requisicao): Promise<T> {
    const url = new URL(req.caminho, this.urlBase);
    for (const [chave, valor] of Object.entries(req.consulta ?? {})) {
      if (valor !== undefined && valor !== '') url.searchParams.set(chave, String(valor));
    }
    const cabecalhos: Record<string, string> = { accept: 'application/json' };
    if (req.contexto) {
      cabecalhos['x-tenant-id'] = req.contexto.tenantId;
      cabecalhos['x-user-id'] = req.contexto.usuarioId;
      cabecalhos['x-correlation-id'] = req.contexto.correlationId;
    }
    if (req.corpo !== undefined) cabecalhos['content-type'] = 'application/json';

    let resposta: Response;
    try {
      resposta = await fetch(url, {
        method: req.metodo,
        headers: cabecalhos,
        body: req.corpo === undefined ? undefined : JSON.stringify(req.corpo),
        signal: AbortSignal.timeout(this.opcoes.timeoutMs),
      });
    } catch {
      throw new ErroHttp(503, 'Dependencia indisponivel', `${this.nome} nao respondeu`);
    }
    const texto = await resposta.text();
    const corpo = texto ? safeJson(texto) : undefined;
    if (!resposta.ok) {
      const detalhe = (corpo as { detail?: string } | undefined)?.detail ?? `${this.nome} respondeu ${resposta.status}`;
      const status = resposta.status >= 500 ? 503 : resposta.status;
      throw new ErroHttp(status, (corpo as { title?: string } | undefined)?.title ?? 'Erro', detalhe, corpo);
    }
    return corpo as T;
  }
}

function safeJson(texto: string): unknown {
  try {
    return JSON.parse(texto);
  } catch {
    return { bruto: texto };
  }
}
