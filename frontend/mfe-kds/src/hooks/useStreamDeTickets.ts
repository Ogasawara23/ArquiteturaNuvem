import { useEffect, useState } from 'react';

/**
 * Assina o stream SSE da estacao. A cada evento OU reconexao chama `recarregar`:
 * o SSE e apenas notificacao, a fonte de verdade e sempre o snapshot (Figura 17).
 */
export function useStreamDeTickets(url: string | null, recarregar: () => void): 'conectado' | 'reconectando' | 'desligado' {
  const [estado, setEstado] = useState<'conectado' | 'reconectando' | 'desligado'>('desligado');
  useEffect(() => {
    if (!url || typeof EventSource === 'undefined') return;
    const fonte = new EventSource(url);
    const aoEvento = () => recarregar();
    fonte.onopen = () => {
      setEstado('conectado');
      recarregar();
    };
    fonte.onerror = () => setEstado('reconectando');
    for (const tipo of ['ticket-criado', 'ticket-atualizado', 'ticket-expedido', 'fila-alterada']) {
      fonte.addEventListener(tipo, aoEvento);
    }
    return () => {
      fonte.close();
      setEstado('desligado');
    };
  }, [url, recarregar]);
  return estado;
}
