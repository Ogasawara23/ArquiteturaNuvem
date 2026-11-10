/**
 * Regra pura (sem SDK do Azure): classifica o momento da operacao do restaurante.
 * O bff-admin usa o multiplicador para estimar o tempo de espera e o painel mostra a faixa.
 */

export type FaixaHoraria = 'madrugada' | 'cafe-da-manha' | 'almoco' | 'tarde' | 'jantar' | 'noite';

export interface StatusOperacional {
  tenantId: string;
  fusoHorario: string;
  horaLocal: string;
  diaDaSemana: string;
  faixaHoraria: FaixaHoraria;
  emHorarioDePico: boolean;
  multiplicadorTempoPreparo: number;
  mensagem: string;
  geradoEm: string;
}

const DIAS = ['domingo', 'segunda', 'terca', 'quarta', 'quinta', 'sexta', 'sabado'];

const FAIXAS: Array<{ ate: number; faixa: FaixaHoraria }> = [
  { ate: 6, faixa: 'madrugada' },
  { ate: 11, faixa: 'cafe-da-manha' },
  { ate: 15, faixa: 'almoco' },
  { ate: 18, faixa: 'tarde' },
  { ate: 23, faixa: 'jantar' },
  { ate: 24, faixa: 'noite' },
];

export class FusoInvalidoError extends Error {}

export function horaEDiaLocais(agora: Date, fusoHorario: string): { hora: number; minuto: number; dia: number } {
  let partes: Intl.DateTimeFormatPart[];
  try {
    partes = new Intl.DateTimeFormat('en-US', {
      timeZone: fusoHorario,
      hour: '2-digit',
      minute: '2-digit',
      weekday: 'short',
      hourCycle: 'h23',
    }).formatToParts(agora);
  } catch {
    throw new FusoInvalidoError(`fuso horario invalido: ${fusoHorario}`);
  }
  const valor = (tipo: string) => partes.find((p) => p.type === tipo)?.value ?? '0';
  const dia = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].indexOf(valor('weekday'));
  return { hora: Number(valor('hour')) % 24, minuto: Number(valor('minute')), dia };
}

export function classificarFaixa(hora: number): FaixaHoraria {
  return FAIXAS.find((f) => hora < f.ate)!.faixa;
}

/** Pico: almoco (11h-14h59) em qualquer dia e jantar (19h-22h59) de quinta a sabado. */
export function emPico(hora: number, dia: number): boolean {
  const almoco = hora >= 11 && hora < 15;
  const jantarDeFimDeSemana = hora >= 19 && hora < 23 && dia >= 4 && dia <= 6;
  return almoco || jantarDeFimDeSemana;
}

export function multiplicador(hora: number, dia: number): number {
  if (emPico(hora, dia)) {
    return dia === 0 || dia === 6 ? 1.5 : 1.3;
  }
  return hora < 6 ? 0.8 : 1.0;
}

export function calcularStatusOperacional(tenantId: string, fusoHorario: string, agora: Date): StatusOperacional {
  const { hora, minuto, dia } = horaEDiaLocais(agora, fusoHorario);
  const pico = emPico(hora, dia);
  const fator = multiplicador(hora, dia);
  const faixa = classificarFaixa(hora);
  return {
    tenantId,
    fusoHorario,
    horaLocal: `${String(hora).padStart(2, '0')}:${String(minuto).padStart(2, '0')}`,
    diaDaSemana: DIAS[dia],
    faixaHoraria: faixa,
    emHorarioDePico: pico,
    multiplicadorTempoPreparo: fator,
    mensagem: pico
      ? `Horario de pico (${faixa}): tempos de preparo estimados ${Math.round((fator - 1) * 100)}% maiores.`
      : `Operacao normal (${faixa}).`,
    geradoEm: agora.toISOString(),
  };
}
