import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { concluirLogin, iniciarLogin, sair, type ConfiguracaoOidc } from './oidcClient';

/** Contrato entregue aos remotes por props (o mesmo tipo e declarado em cada remote). */
export interface Sessao {
  apiUrl: string;
  token?: string;
  tenantId: string;
  usuario: string;
  papeis: string[];
  urlMesa?: string;
}

interface ValorDoContexto {
  sessao: Sessao | null;
  modoDemonstracao: boolean;
  carregando: boolean;
  erro: string | null;
  entrar: (demo?: { tenantId: string; papeis: string[] }) => void;
  sairDaSessao: () => void;
}

const Contexto = createContext<ValorDoContexto | null>(null);

const env = import.meta.env;
const apiUrl: string = env.VITE_API_URL ?? 'http://localhost:8080';
const urlMesa: string = env.VITE_URL_MESA ?? 'http://localhost:5100';
const oidc: ConfiguracaoOidc | null = env.VITE_KEYCLOAK_URL
  ? { emissor: env.VITE_KEYCLOAK_URL, clienteId: env.VITE_KEYCLOAK_CLIENT_ID ?? 'shell-operacao', redirecionamento: window.location.origin + '/' }
  : null;

/** ContextoDeSessao (Figura 6): token, tenantId e papeis, mantidos em memoria. */
export function ProvedorDeSessao({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<Sessao | null>(null);
  const [carregando, setCarregando] = useState(Boolean(oidc));
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    if (!oidc) return;
    concluirLogin(oidc)
      .then((s) => s && setSessao({ apiUrl, urlMesa, token: s.token, tenantId: s.tenantId, usuario: s.usuario, papeis: s.papeis }))
      .catch((e: Error) => setErro(e.message))
      .finally(() => setCarregando(false));
  }, []);

  const valor = useMemo<ValorDoContexto>(() => ({
    sessao,
    modoDemonstracao: !oidc,
    carregando,
    erro,
    entrar: (demo) => {
      if (oidc) void iniciarLogin(oidc);
      else if (demo) setSessao({ apiUrl, urlMesa, tenantId: demo.tenantId, usuario: 'demonstracao', papeis: demo.papeis });
    },
    sairDaSessao: () => (oidc ? sair(oidc) : setSessao(null)),
  }), [sessao, carregando, erro]);

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}

export function useSessao(): ValorDoContexto {
  const valor = useContext(Contexto);
  if (!valor) throw new Error('useSessao fora do ProvedorDeSessao');
  return valor;
}
