import { useCallback, useEffect, useMemo, useState } from 'react';
import { criarClienteKds, type Estacao, type FilaDaEstacao } from './api/bffKdsClient';
import type { Sessao } from './api/sessao';
import { CartaoDeTicket } from './componentes/CartaoDeTicket';
import { useStreamDeTickets } from './hooks/useStreamDeTickets';

/** Modulo exposto ./TelaProducao: Kitchen Display System por estacao (RF12 a RF16). */
export default function TelaProducao({ sessao }: { sessao: Sessao }) {
  const cliente = useMemo(() => criarClienteKds(sessao), [sessao]);
  const [estacoes, setEstacoes] = useState<Estacao[]>([]);
  const [estacao, setEstacao] = useState<string>('');
  const [fila, setFila] = useState<FilaDaEstacao | null>(null);
  const [recebidoEm, setRecebidoEm] = useState(Date.now());
  const [erro, setErro] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);

  useEffect(() => {
    cliente.estacoes()
      .then((lista) => {
        setEstacoes(lista);
        setEstacao((atual) => atual || lista[0]?.codigo || 'grill');
      })
      .catch(() => setEstacao((atual) => atual || 'grill'));
  }, [cliente]);

  const recarregar = useCallback(() => {
    if (!estacao) return;
    cliente.fila(estacao)
      .then((f) => {
        setFila(f);
        setRecebidoEm(Date.now());
        setErro(null);
      })
      .catch((e: Error) => setErro(e.message));
  }, [cliente, estacao]);

  useEffect(recarregar, [recarregar]);
  const conexao = useStreamDeTickets(estacao ? cliente.urlDoStream(estacao) : null, recarregar);

  const executar = (acao: () => Promise<void>) => {
    setOcupado(true);
    acao().then(recarregar).catch((e: Error) => setErro(e.message)).finally(() => setOcupado(false));
  };
  const acoes = {
    iniciar: (id: string) => executar(() => cliente.avancar(id, 'EM_PREPARO')),
    pronto: (id: string) => executar(() => cliente.avancar(id, 'PRONTO')),
    bump: (id: string) => executar(() => cliente.bump(id)),
  };

  return (
    <section className="kds">
      <div className="barra">
        <label>
          Estacao{' '}
          <select value={estacao} onChange={(e) => setEstacao(e.target.value)} aria-label="Estacao">
            {(estacoes.length ? estacoes : [{ codigo: estacao, nome: estacao }]).map((e) => (
              <option key={e.codigo} value={e.codigo}>{e.nome}</option>
            ))}
          </select>
        </label>
        <span className={`badge ${conexao === 'conectado' ? 'badge-normal' : 'badge-atencao'}`}>
          {conexao === 'conectado' ? 'Tempo real ativo' : conexao === 'reconectando' ? 'Reconectando...' : 'Sem tempo real'}
        </span>
        <span className="texto-suave">{fila ? `${fila.tickets.length} ticket(s) em ${fila.estacao.nome}` : 'Carregando...'}</span>
        <button className="btn btn-secundario" onClick={recarregar}>Atualizar</button>
      </div>
      {erro && <div className="alerta" role="alert">{erro}</div>}
      <div className="kds-grade">
        {fila?.tickets.map((t) => (
          <CartaoDeTicket key={t.ticketId} ticket={t} recebidoEm={recebidoEm} acoes={acoes} ocupado={ocupado} />
        ))}
        {fila && fila.tickets.length === 0 && <p className="vazio">Nenhum ticket na fila. Bom trabalho!</p>}
      </div>
    </section>
  );
}
