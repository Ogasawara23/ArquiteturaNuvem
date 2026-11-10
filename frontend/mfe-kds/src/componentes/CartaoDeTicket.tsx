import type { Ticket } from '../api/bffKdsClient';
import { Cronometro, formatarDuracao } from './Cronometro';

const ROTULO_DESTAQUE = { normal: 'No prazo', atencao: 'Atencao', atrasado: 'Atrasado' } as const;

export interface AcoesDoTicket {
  iniciar: (id: string) => void;
  pronto: (id: string) => void;
  bump: (id: string) => void;
}

/** Cartao de alto contraste com alvo de toque grande: um gesto por transicao (persona Cozinheiro). */
export function CartaoDeTicket({ ticket, recebidoEm, acoes, ocupado }: { ticket: Ticket; recebidoEm: number; acoes: AcoesDoTicket; ocupado: boolean }) {
  return (
    <article className={`card kds-ticket kds-${ticket.destaque}`} data-testid="ticket" data-destaque={ticket.destaque}>
      <header className="kds-ticket-topo">
        <strong className="kds-mesa">{ticket.mesa}</strong>
        <span className={`badge badge-${ticket.destaque}`}>{ROTULO_DESTAQUE[ticket.destaque]}</span>
      </header>
      <div className="kds-tempos">
        <Cronometro decorridoInicial={ticket.decorridoSegundos} recebidoEm={recebidoEm} />
        <small> / alvo {formatarDuracao(ticket.tempoAlvoSegundos)}</small>
      </div>
      <ul className="kds-itens">
        {ticket.itens.map((item, i) => (
          <li key={i} className={item.cancelado ? 'kds-cancelado' : undefined}>
            <b>{item.quantidade}x</b> {item.descricao}
            {item.modificadores.length > 0 && <div className="kds-mod">{item.modificadores.join(' · ')}</div>}
            {item.observacao && <div className="kds-obs">⚠ {item.observacao}</div>}
          </li>
        ))}
      </ul>
      <footer>
        {ticket.status === 'RECEBIDO' && (
          <button className="btn btn-grande" disabled={ocupado} onClick={() => acoes.iniciar(ticket.ticketId)}>Iniciar preparo</button>
        )}
        {ticket.status === 'EM_PREPARO' && (
          <button className="btn btn-grande btn-sucesso" disabled={ocupado} onClick={() => acoes.pronto(ticket.ticketId)}>Pronto</button>
        )}
        {ticket.status === 'PRONTO' && (
          <button className="btn btn-grande btn-destaque" disabled={ocupado} onClick={() => acoes.bump(ticket.ticketId)}>Bump (expedir)</button>
        )}
      </footer>
    </article>
  );
}
