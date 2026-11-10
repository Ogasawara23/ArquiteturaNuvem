import { useState, type FormEvent } from 'react';
import type { DadosDoItem } from '../api/bffCardapioClient';
import { escreverGrupos, lerGrupos } from './gruposEmTexto';

const VAZIO: DadosDoItem = {
  nome: '', descricao: '', categoria: 'Lanches', precoCentavos: 0, estacaoSugerida: 'grill',
  tempoAlvoPreparoSegundos: 600, imagemUrl: '', gruposModificadores: [],
};

export function FormularioDeItem({ inicial, aoSalvar, aoCancelar }: {
  inicial?: DadosDoItem;
  aoSalvar: (d: DadosDoItem) => Promise<void>;
  aoCancelar: () => void;
}) {
  const base = inicial ?? VAZIO;
  const [d, setD] = useState({ ...base, preco: (base.precoCentavos / 100).toFixed(2), minutos: String(base.tempoAlvoPreparoSegundos / 60) });
  const [grupos, setGrupos] = useState(escreverGrupos(base.gruposModificadores));
  const [erro, setErro] = useState<string | null>(null);

  const enviar = (e: FormEvent) => {
    e.preventDefault();
    try {
      const dados: DadosDoItem = {
        nome: d.nome, descricao: d.descricao, categoria: d.categoria, estacaoSugerida: d.estacaoSugerida,
        imagemUrl: d.imagemUrl || undefined,
        precoCentavos: Math.round(Number(d.preco.replace(',', '.')) * 100),
        tempoAlvoPreparoSegundos: Math.round(Number(d.minutos.replace(',', '.')) * 60),
        gruposModificadores: lerGrupos(grupos),
      };
      aoSalvar(dados).catch((x: Error) => setErro(x.message));
    } catch (x) {
      setErro((x as Error).message);
    }
  };
  const campo = (rotulo: string, chave: keyof typeof d, extra: Record<string, unknown> = {}) => (
    <label className="campo">
      {rotulo}
      <input value={String(d[chave] ?? '')} onChange={(e) => setD({ ...d, [chave]: e.target.value })} {...extra} />
    </label>
  );

  return (
    <form className="card formulario" onSubmit={enviar}>
      <h3>{inicial ? 'Editar item' : 'Novo item'}</h3>
      <div className="grade-2">
        {campo('Nome', 'nome', { required: true })}
        {campo('Categoria', 'categoria', { required: true })}
        {campo('Preco (R$)', 'preco', { required: true, inputMode: 'decimal' })}
        {campo('Estacao sugerida', 'estacaoSugerida', { required: true })}
        {campo('Tempo-alvo (min)', 'minutos', { required: true, inputMode: 'decimal' })}
        {campo('URL da imagem', 'imagemUrl')}
      </div>
      {campo('Descricao', 'descricao')}
      <label className="campo">
        Modificadores (um grupo por linha: <code>Nome* (min-max): opcao, opcao=preco</code>)
        <textarea rows={3} value={grupos} onChange={(e) => setGrupos(e.target.value)}
          placeholder={'Ponto da carne* (1-1): Ao ponto, Bem passado\nAdicionais (0-3): Bacon extra=6.00'} />
      </label>
      {erro && <div className="alerta" role="alert">{erro}</div>}
      <div className="acoes">
        <button className="btn" type="submit">Salvar</button>
        <button className="btn btn-secundario" type="button" onClick={aoCancelar}>Cancelar</button>
      </div>
    </form>
  );
}
