import type { Contexto } from '../contexto.js';
import { ClienteHttpResiliente } from './clienteHttpResiliente.js';

export interface EstacaoDto {
  id: string;
  codigo: string;
  nome: string;
  ordemExibicao: number;
  percentualAtencao: number;
  ativa: boolean;
}

export class EstabelecimentoClient {
  constructor(private readonly http: ClienteHttpResiliente) {}

  obterEstacao(contexto: Contexto, codigo: string): Promise<EstacaoDto> {
    return this.http.requisitar({ metodo: 'GET', caminho: `/interno/estacoes/${encodeURIComponent(codigo)}`, contexto });
  }

  listarEstacoes(contexto: Contexto): Promise<EstacaoDto[]> {
    return this.http.requisitar({ metodo: 'GET', caminho: '/interno/estacoes', contexto });
  }
}
