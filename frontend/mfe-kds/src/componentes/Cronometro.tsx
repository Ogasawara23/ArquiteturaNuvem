import { useEffect, useState } from 'react';

export function formatarDuracao(segundos: number): string {
  const s = Math.max(0, Math.floor(segundos));
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
}

/** Conta localmente a partir do decorrido calculado no servidor (sem polling). */
export function Cronometro({ decorridoInicial, recebidoEm }: { decorridoInicial: number; recebidoEm: number }) {
  const [agora, setAgora] = useState(Date.now());
  useEffect(() => {
    const t = setInterval(() => setAgora(Date.now()), 1000);
    return () => clearInterval(t);
  }, []);
  return <span className="kds-cronometro">{formatarDuracao(decorridoInicial + (agora - recebidoEm) / 1000)}</span>;
}
