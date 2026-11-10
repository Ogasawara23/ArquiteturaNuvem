import { useCallback, useEffect, useMemo, useState } from 'react';
import { criarClienteCardapio, type DadosDoItem, type ItemResumo } from './api/bffCardapioClient';
import type { Sessao } from './api/sessao';
import { FormularioDeItem } from './componentes/FormularioDeItem';
import { reais } from './componentes/gruposEmTexto';

type Edicao = { modo: 'novo' } | { modo: 'editar'; id: string; dados: DadosDoItem } | null;

/** Modulo exposto ./TelaCardapio: CRUD de itens e publicacao de versao imutavel (RF02, RF03). */
export default function TelaCardapio({ sessao }: { sessao: Sessao }) {
  const api = useMemo(() => criarClienteCardapio(sessao), [sessao]);
  const [itens, setItens] = useState<ItemResumo[]>([]);
  const [ativo, setAtivo] = useState<{ versaoId: string; publicadaEm: string } | null>(null);
  const [edicao, setEdicao] = useState<Edicao>(null);
  const [mensagem, setMensagem] = useState<{ tipo: 'ok' | 'erro'; texto: string } | null>(null);

  const recarregar = useCallback(() => {
    api.listar().then(setItens).catch((e: Error) => setMensagem({ tipo: 'erro', texto: e.message }));
    api.ativo().then(setAtivo).catch(() => setAtivo(null));
  }, [api]);
  useEffect(recarregar, [recarregar]);

  const tentar = (acao: () => Promise<unknown>, sucesso: string) =>
    acao().then(() => { setMensagem({ tipo: 'ok', texto: sucesso }); recarregar(); })
      .catch((e: Error) => setMensagem({ tipo: 'erro', texto: e.message }));

  const salvar = async (dados: DadosDoItem) => {
    if (edicao?.modo === 'editar') await api.atualizar(edicao.id, dados);
    else await api.cadastrar(dados);
    setEdicao(null);
    setMensagem({ tipo: 'ok', texto: 'Item salvo. Publique uma nova versao para o cliente ver a mudanca.' });
    recarregar();
  };

  const categorias = [...new Set(itens.map((i) => i.categoria))];

  return (
    <section>
      <div className="barra">
        <h2>Cardapio</h2>
        <span className="texto-suave">
          {ativo ? `Versao ativa ${ativo.versaoId} (${new Date(ativo.publicadaEm).toLocaleString('pt-BR')})` : 'Nenhuma versao publicada'}
        </span>
        <button className="btn btn-secundario" onClick={() => setEdicao({ modo: 'novo' })}>+ Novo item</button>
        <button className="btn btn-destaque" onClick={() => tentar(api.publicar, 'Nova versao publicada: comandas abertas mantem o preco antigo.')}>
          Publicar versao
        </button>
      </div>
      {mensagem && <div className={mensagem.tipo === 'ok' ? 'sucesso' : 'alerta'} role="status">{mensagem.texto}</div>}
      {edicao && (
        <FormularioDeItem key={edicao.modo === 'editar' ? edicao.id : 'novo'} inicial={edicao.modo === 'editar' ? edicao.dados : undefined}
          aoSalvar={salvar} aoCancelar={() => setEdicao(null)} />
      )}
      {categorias.map((cat) => (
        <div key={cat} className="card">
          <h3>{cat}</h3>
          <table className="tabela">
            <thead><tr><th>Item</th><th>Preco</th><th>Estacao</th><th>Tempo-alvo</th><th>Situacao</th><th /></tr></thead>
            <tbody>
              {itens.filter((i) => i.categoria === cat).map((i) => (
                <tr key={i.id}>
                  <td>{i.nome}</td>
                  <td>{reais(i.precoCentavos)}</td>
                  <td>{i.estacaoSugerida}</td>
                  <td>{Math.round(i.tempoAlvoPreparoSegundos / 60)} min</td>
                  <td><span className={`badge ${i.disponivel ? 'badge-normal' : 'badge-atrasado'}`}>{i.disponivel ? 'Disponivel' : 'Esgotado'}</span></td>
                  <td className="acoes">
                    <button className="btn btn-pequeno btn-secundario" onClick={() => api.obter(i.id).then((d) => setEdicao({ modo: 'editar', id: i.id, dados: d }))}>Editar</button>
                    <button className="btn btn-pequeno btn-secundario" onClick={() => tentar(() => api.disponibilidade(i.id, !i.disponivel), 'Disponibilidade alterada')}>
                      {i.disponivel ? 'Esgotar' : 'Reativar'}
                    </button>
                    <button className="btn btn-pequeno btn-perigo" onClick={() => confirm(`Remover ${i.nome}?`) && tentar(() => api.remover(i.id), 'Item removido')}>Remover</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ))}
      {itens.length === 0 && !edicao && <p className="vazio">Nenhum item cadastrado ainda.</p>}
    </section>
  );
}
