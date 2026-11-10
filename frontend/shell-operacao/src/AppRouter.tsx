import { useEffect, useState } from 'react';
import { RemoteLoader } from './remotes/RemoteLoader';
import { remotesPermitidos } from './remotes/remotes.config';
import { useSessao } from './sessao/ContextoDeSessao';

const PERFIS_DEMO: Array<{ rotulo: string; papeis: string[] }> = [
  { rotulo: 'Gerente', papeis: ['GERENTE'] },
  { rotulo: 'Cozinha', papeis: ['COZINHA'] },
  { rotulo: 'Atendente', papeis: ['ATENDENTE'] },
];

function useRotaAtual(): [string, (r: string) => void] {
  const ler = () => window.location.hash.replace(/^#\/?/, '');
  const [rota, setRota] = useState(ler());
  useEffect(() => {
    const aoMudar = () => setRota(ler());
    window.addEventListener('hashchange', aoMudar);
    return () => window.removeEventListener('hashchange', aoMudar);
  }, []);
  return [rota, (r) => { window.location.hash = `/${r}`; }];
}

function Entrada() {
  const { entrar, modoDemonstracao, erro } = useSessao();
  const [tenant, setTenant] = useState('demo');
  return (
    <main className="entrada card">
      <h1>SmartKDS &amp; Menu</h1>
      <p className="texto-suave">Operacao do restaurante: cozinha, cardapio e gestao.</p>
      {erro && <div className="alerta">{erro}</div>}
      {modoDemonstracao ? (
        <>
          <p><b>Modo demonstracao</b> (Keycloak nao configurado): escolha o estabelecimento e o perfil.</p>
          <label className="campo">Estabelecimento (tenant)<input value={tenant} onChange={(e) => setTenant(e.target.value)} /></label>
          <div className="acoes">
            {PERFIS_DEMO.map((p) => (
              <button key={p.rotulo} className="btn" onClick={() => entrar({ tenantId: tenant, papeis: p.papeis })}>Entrar como {p.rotulo}</button>
            ))}
          </div>
        </>
      ) : (
        <button className="btn btn-grande" onClick={() => entrar()}>Entrar com Keycloak</button>
      )}
    </main>
  );
}

/** AppRouter (Figura 6): rotas de topo do shell; cada rota carrega um remote. */
export function AppRouter() {
  const { sessao, carregando, sairDaSessao } = useSessao();
  const [rota, navegar] = useRotaAtual();
  if (carregando) return <p className="texto-suave">Autenticando...</p>;
  if (!sessao) return <Entrada />;

  const permitidos = remotesPermitidos(sessao.papeis);
  const atual = permitidos.find((r) => r.id === rota) ?? permitidos[0];
  return (
    <div className="shell">
      <header className="shell-topo">
        <b className="marca">SmartKDS</b>
        <nav>
          {permitidos.map((r) => (
            <button key={r.id} className={r.id === atual?.id ? 'aba ativa' : 'aba'} onClick={() => navegar(r.id)}>{r.titulo}</button>
          ))}
        </nav>
        <span className="texto-suave usuario">{sessao.usuario} @ {sessao.tenantId}</span>
        <button className="btn btn-pequeno btn-secundario" onClick={sairDaSessao}>Sair</button>
      </header>
      <main className="shell-conteudo">
        {atual ? <RemoteLoader nome={atual.titulo} Componente={atual.Componente} props={{ sessao }} />
          : <p className="vazio">Seu perfil nao tem acesso a nenhum modulo.</p>}
      </main>
    </div>
  );
}
