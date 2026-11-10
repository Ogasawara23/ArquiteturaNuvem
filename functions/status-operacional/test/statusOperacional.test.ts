import { describe, expect, it } from 'vitest';
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import {
  calcularStatusOperacional,
  classificarFaixa,
  emPico,
  FusoInvalidoError,
} from '../src/features/status-operacional/calcularStatusOperacional';

describe('status operacional (regra pura)', () => {
  it('sexta 20h em Sao Paulo e pico de jantar', () => {
    // 2026-10-16 e sexta-feira; 23:00Z = 20:00 em America/Sao_Paulo
    const s = calcularStatusOperacional('demo', 'America/Sao_Paulo', new Date('2026-10-16T23:00:00Z'));
    expect(s.diaDaSemana).toBe('sexta');
    expect(s.horaLocal).toBe('20:00');
    expect(s.faixaHoraria).toBe('jantar');
    expect(s.emHorarioDePico).toBe(true);
    expect(s.multiplicadorTempoPreparo).toBe(1.3);
  });

  it('terca 16h e operacao normal', () => {
    const s = calcularStatusOperacional('demo', 'America/Sao_Paulo', new Date('2026-10-13T19:00:00Z'));
    expect(s.emHorarioDePico).toBe(false);
    expect(s.multiplicadorTempoPreparo).toBe(1.0);
  });

  it('almoco de sabado tem o maior multiplicador', () => {
    expect(emPico(12, 6)).toBe(true);
    expect(calcularStatusOperacional('t', 'UTC', new Date('2026-10-17T12:30:00Z')).multiplicadorTempoPreparo).toBe(1.5);
  });

  it('faixas cobrem as 24 horas', () => {
    expect(Array.from({ length: 24 }, (_, h) => classificarFaixa(h))).not.toContain(undefined);
  });

  it('fuso invalido e erro de entrada', () => {
    expect(() => calcularStatusOperacional('t', 'Lua/Base', new Date())).toThrow(FusoInvalidoError);
  });
});

describe('arquitetura da function', () => {
  it('a feature nao depende do SDK do Azure (regra pura e testavel)', () => {
    const pasta = join(__dirname, '..', 'src', 'features');
    const arquivos = readdirSync(pasta, { recursive: true }).filter((f) => String(f).endsWith('.ts'));
    for (const arquivo of arquivos) {
      const codigo = readFileSync(join(pasta, String(arquivo)), 'utf-8');
      expect(codigo, String(arquivo)).not.toMatch(/from ['"]@azure\/functions['"]/);
    }
  });
});
