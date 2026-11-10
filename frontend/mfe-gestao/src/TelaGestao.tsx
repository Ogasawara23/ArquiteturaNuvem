import { useCallback, useMemo, useState } from 'react';
import { criarClienteGestao } from './api/bffGestaoClient';
import type { Sessao } from './api/sessao';
import { Auditoria, Comandas, Estacoes, Mesas } from './componentes/Operacao';
import { Painel } from './componentes/Painel';

const ABAS = ['Painel', 'Comandas', 'Mesas', 'Estacoes e roteamento', 'Auditoria'] as const;
type Aba = (typeof ABAS)[number];

/** Modulo exposto ./TelaGestao: RF10, RF18, RF19 e RF20. */
export default function TelaGestao({ sessao }: { sessao: Sessao }) {
  const api = useMemo(() => criarClienteGestao(sessao), [sessao]);
  const [aba, setAba] = useState<Aba>('Painel');
  const [mensagem, setMensagem] = useState<{ tipo: 'ok' | 'erro'; texto: string } | null>(null);
  const aviso = useCallback((tipo: 'ok' | 'erro', texto: string) => setMensagem({ tipo, texto }), []);
  const urlMesa = sessao.urlMesa ?? window.location.origin;

  return (
    <section>
      <nav className="abas" role="tablist">
        {ABAS.map((a) => (
          <button key={a} role="tab" aria-selected={a === aba} className={a === aba ? 'aba ativa' : 'aba'} onClick={() => { setAba(a); setMensagem(null); }}>{a}</button>
        ))}
      </nav>
      {mensagem && <div className={mensagem.tipo === 'ok' ? 'sucesso' : 'alerta'} role="status">{mensagem.texto}</div>}
      {aba === 'Painel' && <Painel api={api} />}
      {aba === 'Comandas' && <Comandas api={api} aviso={aviso} />}
      {aba === 'Mesas' && <Mesas api={api} aviso={aviso} urlMesa={urlMesa} />}
      {aba === 'Estacoes e roteamento' && <Estacoes api={api} aviso={aviso} />}
      {aba === 'Auditoria' && <Auditoria api={api} aviso={aviso} />}
    </section>
  );
}
