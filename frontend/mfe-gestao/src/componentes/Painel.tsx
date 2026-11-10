import { useEffect, useState } from 'react';
import type { ClienteGestao, DadosAgregados } from '../api/bffGestaoClient';
import { minutos, reais } from './formatos';

/** Painel do gerente: consome GET /bff/admin/aggregated-data (MS1 + MS2 + Azure Function). */
export function Painel({ api }: { api: ClienteGestao }) {
  const [dados, setDados] = useState<DadosAgregados | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  useEffect(() => {
    const carregar = () => api.agregado().then((d) => { setDados(d); setErro(null); }).catch((e: Error) => setErro(e.message));
    carregar();
    const t = setInterval(carregar, 15_000);
    return () => clearInterval(t);
  }, [api]);

  if (erro) return <div className="alerta">{erro}</div>;
  if (!dados) return <p>Carregando painel...</p>;
  const maiorVolume = Math.max(1, ...(dados.producao?.volumePorHora.map((v) => v.tickets) ?? [1]));

  return (
    <div>
      <div className="kpis">
        <div className="card kpi"><small>Comandas abertas</small><b>{dados.pedidos?.comandasAbertas ?? '-'}</b><span>{reais(dados.pedidos?.valorEmAbertoCentavos)} em aberto</span></div>
        <div className="card kpi"><small>Faturamento 24h</small><b>{reais(dados.pedidos?.faturamentoNasUltimas24hCentavos)}</b><span>{dados.pedidos?.comandasFechadasNasUltimas24h ?? 0} comandas fechadas</span></div>
        <div className="card kpi"><small>Itens na cozinha</small><b>{dados.pedidos?.itensAguardandoCozinha ?? '-'}</b><span>confirmados ou em preparo</span></div>
        <div className="card kpi"><small>Cardapio</small><b>{dados.catalogo?.itensDisponiveis ?? '-'} / {dados.catalogo?.totalDeItens ?? '-'}</b><span>itens disponiveis</span></div>
        <div className="card kpi"><small>Tempo estimado</small><b>{minutos(dados.estimativa.tempoEstimadoComPicoSegundos)}</b><span title={dados.estimativa.explicacao}>{dados.estimativa.explicacao}</span></div>
      </div>

      {dados.statusOperacional && (
        <div className={`card ${dados.statusOperacional.emHorarioDePico ? 'destaque-pico' : ''}`}>
          <b>Status operacional (Azure Function):</b> {dados.statusOperacional.diaDaSemana}, {dados.statusOperacional.horaLocal} - {dados.statusOperacional.mensagem}
        </div>
      )}

      <div className="grade-2">
        <div className="card">
          <h3>Producao por estacao (24h)</h3>
          <table className="tabela">
            <thead><tr><th>Estacao</th><th>Ativos</th><th>Expedidos</th><th>Atrasados</th><th>Tempo medio</th></tr></thead>
            <tbody>
              {dados.producao?.estacoes.map((e) => (
                <tr key={e.estacaoId}>
                  <td>{e.estacaoId}</td><td>{e.ticketsAtivos}</td><td>{e.ticketsExpedidos}</td>
                  <td>{e.ticketsAtrasados} ({Math.round(e.taxaDeAtraso * 100)}%)</td><td>{minutos(e.tempoMedioPreparoSegundos)}</td>
                </tr>
              )) ?? <tr><td colSpan={5}>producao-service indisponivel</td></tr>}
            </tbody>
          </table>
          <h4>Volume por hora</h4>
          <div className="barras" role="img" aria-label="Tickets por hora">
            {dados.producao?.volumePorHora.map((v) => (
              <div key={v.hora} className="barra-coluna" title={`${v.hora}h: ${v.tickets} tickets`}>
                <div style={{ height: `${(v.tickets / maiorVolume) * 100}%` }} />
                <small>{v.hora}h</small>
              </div>
            ))}
          </div>
        </div>
        <div className="card">
          <h3>Fontes desta resposta</h3>
          <ul className="lista-fontes">
            {dados.fontes.map((f) => (
              <li key={f.nome}>
                <span className={`badge ${f.situacao === 'ok' ? 'badge-normal' : 'badge-atrasado'}`}>{f.situacao}</span>{' '}
                <b>{f.nome}</b> <small>({f.tipo}, {f.latenciaMs} ms)</small>
                {f.erro && <div className="texto-suave">{f.erro}</div>}
              </li>
            ))}
          </ul>
          <h4>Itens por categoria</h4>
          <ul>{dados.catalogo?.porCategoria.map((c) => <li key={c.categoria}>{c.categoria}: {c.itens}</li>)}</ul>
          <small className="texto-suave">Atualizado em {new Date(dados.geradoEm).toLocaleTimeString('pt-BR')} (a cada 15 s)</small>
        </div>
      </div>
    </div>
  );
}
