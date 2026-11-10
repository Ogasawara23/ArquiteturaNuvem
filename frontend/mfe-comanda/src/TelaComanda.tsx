import { useCallback, useEffect, useMemo, useState } from 'react';
import { criarClienteMesa, type Comanda, type ItemDoCardapio, type NovoItem, type TelaDaMesa } from './api/bffMesaClient';
import { ModalDoItem } from './componentes/ModalDoItem';
import { reais, ROTULO_STATUS } from './componentes/regras';

/** Modulo exposto ./TelaComanda: cardapio, carrinho, envio a cozinha e acompanhamento em tempo real (RF04 a RF09, RF16). */
export default function TelaComanda({ apiUrl, qrToken }: { apiUrl: string; qrToken: string }) {
  const api = useMemo(() => criarClienteMesa(apiUrl, qrToken), [apiUrl, qrToken]);
  const [tela, setTela] = useState<TelaDaMesa | null>(null);
  const [comanda, setComanda] = useState<Comanda | null>(null);
  const [itemAberto, setItemAberto] = useState<ItemDoCardapio | null>(null);
  const [chaveDeEnvio, setChaveDeEnvio] = useState<string | null>(null);
  const [mensagem, setMensagem] = useState<{ tipo: 'ok' | 'erro'; texto: string } | null>(null);
  const [erroFatal, setErroFatal] = useState<string | null>(null);

  useEffect(() => {
    api.tela().then((t) => { setTela(t); setComanda(t.comanda); }).catch((e: Error) => setErroFatal(e.message));
  }, [api]);

  const recarregarComanda = useCallback(() => {
    api.comanda().then((c) => setComanda(c ?? null)).catch(() => undefined);
  }, [api]);

  // SSE: notificacao "comanda-atualizada" -> recarrega o estado (SSE nunca e a unica fonte de verdade)
  const comandaId = comanda?.comandaId;
  useEffect(() => {
    if (!comandaId || typeof EventSource === 'undefined') return;
    const fonte = new EventSource(api.urlDoStream());
    fonte.addEventListener('comanda-atualizada', recarregarComanda);
    fonte.onopen = recarregarComanda;
    return () => fonte.close();
  }, [api, comandaId, recarregarComanda]);

  const adicionar = async (novo: NovoItem) => {
    setComanda(await api.adicionar(novo));
    setItemAberto(null);
    setMensagem({ tipo: 'ok', texto: 'Item adicionado ao carrinho' });
  };

  const enviar = () => {
    // a mesma chave e reutilizada em caso de nova tentativa: toque duplo nao duplica o pedido
    const chave = chaveDeEnvio ?? crypto.randomUUID();
    setChaveDeEnvio(chave);
    api.confirmar(chave)
      .then(() => { setChaveDeEnvio(null); setMensagem({ tipo: 'ok', texto: 'Pedido enviado para a cozinha!' }); recarregarComanda(); })
      .catch((e: Error) => setMensagem({ tipo: 'erro', texto: e.message }));
  };

  const cancelar = (itemId: string) => api.cancelar(itemId)
    .then(() => { setMensagem({ tipo: 'ok', texto: 'Item removido do pedido' }); recarregarComanda(); })
    .catch((e: Error) => setMensagem({ tipo: 'erro', texto: e.message }));

  if (erroFatal) return <div className="mesa-erro"><h2>Ops!</h2><p>{erroFatal}</p></div>;
  if (!tela) return <p className="carregando">Carregando cardapio...</p>;

  const ativos = comanda?.itens.filter((i) => i.status !== 'CANCELADO') ?? [];
  const pendentes = ativos.filter((i) => i.status === 'PENDENTE');

  return (
    <div className="mesa">
      <header className="mesa-topo">
        <div><b>{tela.mesa.estabelecimento}</b><small>{tela.mesa.rotulo}</small></div>
      </header>
      {tela.avisos.map((a) => <div key={a} className="alerta">{a}</div>)}
      {mensagem && <div className={mensagem.tipo === 'ok' ? 'sucesso' : 'alerta'} role="status" onClick={() => setMensagem(null)}>{mensagem.texto}</div>}

      {tela.cardapio?.categorias.map((cat) => (
        <section key={cat.nome}>
          <h2 className="categoria">{cat.nome}</h2>
          <ul className="itens">
            {cat.itens.map((item) => (
              <li key={item.itemCardapioId}>
                <button className="item-cardapio" onClick={() => setItemAberto(item)}>
                  {item.imagemUrl && <img src={item.imagemUrl} alt="" loading="lazy" />}
                  <span className="item-texto"><b>{item.nome}</b>{item.descricao && <small>{item.descricao}</small>}</span>
                  <span className="preco">{reais(item.precoCentavos)}</span>
                </button>
              </li>
            ))}
          </ul>
        </section>
      ))}

      {ativos.length > 0 && (
        <aside className="carrinho card">
          <h2>Seu pedido</h2>
          <ul>
            {ativos.map((i) => (
              <li key={i.itemId}>
                <div><b>{i.quantidade}x {i.descricao}</b>{i.modificadores.length > 0 && <small> ({i.modificadores.join(', ')})</small>}</div>
                <div className="linha-item">
                  <span className={`badge status-${i.status.toLowerCase()}`}>{ROTULO_STATUS[i.status]}</span>
                  <span>{reais(i.totalCentavos)}</span>
                  {i.podeSerCancelado && <button className="btn btn-pequeno btn-secundario" onClick={() => cancelar(i.itemId)}>Remover</button>}
                </div>
              </li>
            ))}
          </ul>
          <p className="total">Subtotal <b>{reais(comanda?.subtotalCentavos ?? 0)}</b></p>
          <small className="texto-suave">Taxa de servico de {tela.mesa.percentualTaxaServico}% aplicada no fechamento.</small>
          {pendentes.length > 0 && (
            <button className="btn btn-grande btn-destaque" onClick={enviar}>Enviar {pendentes.length} item(ns) para a cozinha</button>
          )}
        </aside>
      )}
      {itemAberto && <ModalDoItem item={itemAberto} aoAdicionar={adicionar} aoFechar={() => setItemAberto(null)} />}
    </div>
  );
}
