import { Component, lazy, Suspense, type ReactNode } from 'react';
import { qrTokenDaUrl } from './qrToken';

const TelaComanda = lazy(() => import('comanda/TelaComanda'));
const apiUrl: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

class LimiteDeErro extends Component<{ children: ReactNode }, { falhou: boolean }> {
  state = { falhou: false };
  static getDerivedStateFromError() {
    return { falhou: true };
  }
  render() {
    return this.state.falhou
      ? <div className="mesa-erro"><h2>Cardapio indisponivel</h2><p>Tente novamente em instantes ou chame o atendente.</p></div>
      : this.props.children;
  }
}

/** Host minimo e publico: o qrToken da URL e a credencial de escopo limitado da mesa (Secao 5.1). */
export function AppMesa() {
  const qrToken = qrTokenDaUrl(window.location);
  if (!qrToken) {
    return <div className="mesa-erro"><h2>QR Code invalido</h2><p>Aponte a camera para o QR Code da sua mesa ou chame o atendente.</p></div>;
  }
  return (
    <LimiteDeErro>
      <Suspense fallback={<p className="carregando">Carregando cardapio...</p>}>
        <TelaComanda apiUrl={apiUrl} qrToken={qrToken} />
      </Suspense>
    </LimiteDeErro>
  );
}
