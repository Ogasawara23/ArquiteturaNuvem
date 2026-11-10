import QRCode from 'qrcode';
import { useCallback, useEffect, useState } from 'react';
import type { ClienteGestao, Comanda, Estacao, Mesa, RegistroAuditoria } from '../api/bffGestaoClient';
import { escreverRegras, lerRegras, reais } from './formatos';

type Aviso = (tipo: 'ok' | 'erro', texto: string) => void;

export function Comandas({ api, aviso }: { api: ClienteGestao; aviso: Aviso }) {
  const [comandas, setComandas] = useState<Comanda[]>([]);
  const [taxa, setTaxa] = useState('10');
  const recarregar = useCallback(() => { api.comandas('ABERTA').then(setComandas).catch((e: Error) => aviso('erro', e.message)); }, [api, aviso]);
  useEffect(recarregar, [recarregar]);
  return (
    <div>
      <div className="barra">
        <label>Taxa de servico (%) <input className="input-curto" value={taxa} onChange={(e) => setTaxa(e.target.value)} /></label>
        <button className="btn btn-secundario" onClick={recarregar}>Atualizar</button>
      </div>
      {comandas.length === 0 && <p className="vazio">Nenhuma comanda aberta.</p>}
      <div className="grade-3">
        {comandas.map((c) => (
          <div key={c.comandaId} className="card">
            <h3>{c.mesaRotulo}</h3>
            <ul className="itens-comanda">
              {c.itens.map((i, n) => (
                <li key={n}><span>{i.quantidade}x {i.descricao}</span> <span className="badge">{i.status}</span></li>
              ))}
            </ul>
            <p><b>Subtotal:</b> {reais(c.subtotalCentavos)}</p>
            <button className="btn" onClick={() => api.fechar(c.comandaId, Number(taxa.replace(',', '.')))
              .then((r) => { aviso('ok', `${c.mesaRotulo} fechada: total ${reais(r.totalCentavos)}`); recarregar(); })
              .catch((e: Error) => aviso('erro', e.message))}>Fechar conta</button>{' '}
            <button className="btn btn-secundario" onClick={() => {
              const motivo = prompt(`Cancelar a comanda da ${c.mesaRotulo}? Informe o motivo:`);
              if (motivo !== null) {
                api.cancelarComanda(c.comandaId, motivo)
                  .then(() => { aviso('ok', `Comanda da ${c.mesaRotulo} cancelada`); recarregar(); })
                  .catch((e: Error) => aviso('erro', e.message));
              }
            }}>Cancelar comanda</button>
          </div>
        ))}
      </div>
    </div>
  );
}

function QrDaMesa({ url }: { url: string }) {
  const [imagem, setImagem] = useState('');
  useEffect(() => { QRCode.toDataURL(url, { margin: 1, width: 132 }).then(setImagem).catch(() => setImagem('')); }, [url]);
  return imagem ? <img src={imagem} alt={`QR Code ${url}`} width={132} height={132} /> : null;
}

export function Mesas({ api, aviso, urlMesa }: { api: ClienteGestao; aviso: Aviso; urlMesa: string }) {
  const [mesas, setMesas] = useState<Mesa[]>([]);
  const [rotulo, setRotulo] = useState('');
  const recarregar = useCallback(() => { api.mesas().then(setMesas).catch((e: Error) => aviso('erro', e.message)); }, [api, aviso]);
  useEffect(recarregar, [recarregar]);
  return (
    <div>
      <form className="barra" onSubmit={(e) => { e.preventDefault(); api.criarMesa(rotulo).then(() => { setRotulo(''); recarregar(); }).catch((x: Error) => aviso('erro', x.message)); }}>
        <input placeholder="Rotulo (ex.: Mesa 7)" value={rotulo} onChange={(e) => setRotulo(e.target.value)} required />
        <button className="btn">Cadastrar mesa</button>
      </form>
      <div className="grade-3">
        {mesas.map((m) => {
          const url = `${urlMesa.replace(/\/$/, '')}/m/${m.qrToken}`;
          return (
            <div key={m.mesaId} className="card mesa">
              <h3>{m.rotulo}</h3>
              <QrDaMesa url={url} />
              <a href={url} target="_blank" rel="noreferrer" className="link-mesa">{url}</a>
              <button className="btn btn-pequeno btn-secundario" onClick={() => confirm('Gerar novo QR Code? O impresso deixara de funcionar.')
                && api.rotacionar(m.mesaId).then(recarregar).catch((e: Error) => aviso('erro', e.message))}>Rotacionar QR</button>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export function Estacoes({ api, aviso }: { api: ClienteGestao; aviso: Aviso }) {
  const [estacoes, setEstacoes] = useState<Estacao[]>([]);
  const [nome, setNome] = useState('');
  const [codigo, setCodigo] = useState('');
  const [regras, setRegras] = useState('');
  const recarregar = useCallback(() => {
    api.estacoes().then(setEstacoes).catch((e: Error) => aviso('erro', e.message));
    api.roteamento().then((r) => setRegras(escreverRegras(r))).catch(() => undefined);
  }, [api, aviso]);
  useEffect(recarregar, [recarregar]);
  const salvar = () => {
    try {
      api.salvarRoteamento(lerRegras(regras)).then(() => { aviso('ok', 'Roteamento salvo (aplicado a novos pedidos em ate 5 min)'); recarregar(); })
        .catch((e: Error) => aviso('erro', e.message));
    } catch (e) { aviso('erro', (e as Error).message); }
  };
  return (
    <div className="grade-2">
      <div className="card">
        <h3>Estacoes</h3>
        <ul>{estacoes.map((e) => <li key={e.codigo}><b>{e.nome}</b> <code>{e.codigo}</code> <small>atencao em {e.percentualAtencao}%</small></li>)}</ul>
        <form className="barra" onSubmit={(e) => { e.preventDefault(); api.criarEstacao(nome, codigo).then(() => { setNome(''); setCodigo(''); recarregar(); }).catch((x: Error) => aviso('erro', x.message)); }}>
          <input placeholder="Nome" value={nome} onChange={(e) => setNome(e.target.value)} required />
          <input placeholder="codigo (opcional)" value={codigo} onChange={(e) => setCodigo(e.target.value)} />
          <button className="btn">Adicionar</button>
        </form>
      </div>
      <div className="card">
        <h3>Roteamento item / categoria para estacao</h3>
        <textarea rows={8} value={regras} onChange={(e) => setRegras(e.target.value)}
          placeholder={'padrao -> cozinha\ncategoria:Bebidas -> bar\nitem:<id-do-item> -> grill (5)'} />
        <p className="texto-suave">Precedencia: regra do item, regra da categoria, estacao sugerida no cardapio, estacao padrao.</p>
        <button className="btn" onClick={salvar}>Salvar roteamento</button>
      </div>
    </div>
  );
}

export function Auditoria({ api, aviso }: { api: ClienteGestao; aviso: Aviso }) {
  const [registros, setRegistros] = useState<RegistroAuditoria[]>([]);
  useEffect(() => { api.auditoria().then(setRegistros).catch((e: Error) => aviso('erro', e.message)); }, [api, aviso]);
  return (
    <table className="tabela card">
      <thead><tr><th>Quando</th><th>Acao</th><th>Usuario</th><th>Detalhe</th></tr></thead>
      <tbody>
        {registros.map((r) => (
          <tr key={r.id}>
            <td>{new Date(r.ocorridoEm).toLocaleString('pt-BR')}</td><td>{r.acao}</td><td>{r.usuarioId}</td>
            <td><code>{JSON.stringify(r.detalhe)}</code></td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
