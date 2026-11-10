import { useState } from 'react';
import type { ItemDoCardapio, NovoItem } from '../api/bffMesaClient';
import { precoComAdicionais, reais, validarSelecoes } from './regras';

export function ModalDoItem({ item, aoAdicionar, aoFechar }: {
  item: ItemDoCardapio;
  aoAdicionar: (novo: NovoItem) => Promise<void>;
  aoFechar: () => void;
}) {
  const [selecoes, setSelecoes] = useState<Record<string, string[]>>({});
  const [quantidade, setQuantidade] = useState(1);
  const [observacao, setObservacao] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const alternar = (grupo: string, opcao: string, unica: boolean) => setSelecoes((s) => {
    const atuais = s[grupo] ?? [];
    if (unica) return { ...s, [grupo]: [opcao] };
    return { ...s, [grupo]: atuais.includes(opcao) ? atuais.filter((o) => o !== opcao) : [...atuais, opcao] };
  });

  const adicionar = () => {
    const problema = validarSelecoes(item.gruposModificadores, selecoes);
    if (problema) return setErro(problema);
    setEnviando(true);
    aoAdicionar({
      itemCardapioId: item.itemCardapioId,
      quantidade,
      observacao: observacao.trim() || undefined,
      modificadores: Object.entries(selecoes).flatMap(([grupo, opcoes]) => opcoes.map((opcao) => ({ grupo, opcao }))),
    }).catch((e: Error) => setErro(e.message)).finally(() => setEnviando(false));
  };

  return (
    <div className="modal-fundo" role="dialog" aria-modal="true" aria-label={item.nome} onClick={aoFechar}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>{item.nome}</h3>
        {item.descricao && <p className="texto-suave">{item.descricao}</p>}
        {item.gruposModificadores.map((g) => {
          const unica = g.maximoSelecoes === 1;
          return (
            <fieldset key={g.nome} className="grupo">
              <legend>{g.nome} {g.obrigatorio && <span className="badge badge-atencao">obrigatorio</span>}</legend>
              {g.opcoes.map((o) => (
                <label key={o.nome} className={`opcao ${o.disponivel ? '' : 'esgotada'}`}>
                  <input type={unica ? 'radio' : 'checkbox'} name={g.nome} disabled={!o.disponivel}
                    checked={selecoes[g.nome]?.includes(o.nome) ?? false} onChange={() => alternar(g.nome, o.nome, unica)} />
                  {o.nome} {o.precoAdicionalCentavos > 0 && <small>+ {reais(o.precoAdicionalCentavos)}</small>}
                  {!o.disponivel && <small> (esgotado)</small>}
                </label>
              ))}
            </fieldset>
          );
        })}
        <label className="campo">Observacao<input maxLength={200} value={observacao} onChange={(e) => setObservacao(e.target.value)} placeholder="ex.: sem cebola" /></label>
        <div className="quantidade">
          <button className="btn btn-secundario" onClick={() => setQuantidade((q) => Math.max(1, q - 1))} aria-label="Diminuir">-</button>
          <b>{quantidade}</b>
          <button className="btn btn-secundario" onClick={() => setQuantidade((q) => Math.min(99, q + 1))} aria-label="Aumentar">+</button>
        </div>
        {erro && <div className="alerta" role="alert">{erro}</div>}
        <div className="acoes">
          <button className="btn btn-grande" disabled={enviando} onClick={adicionar}>
            Adicionar {reais(precoComAdicionais(item.precoCentavos, item.gruposModificadores, selecoes, quantidade))}
          </button>
          <button className="btn btn-secundario" onClick={aoFechar}>Voltar</button>
        </div>
      </div>
    </div>
  );
}
