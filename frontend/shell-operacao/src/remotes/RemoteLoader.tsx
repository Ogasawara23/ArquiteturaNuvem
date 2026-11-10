import { Component, Suspense, type ComponentType, type ReactNode } from 'react';

/**
 * Isolamento de falha (ADR-01): se um remote estiver fora do ar ou quebrar em runtime,
 * apenas a sua area mostra o aviso; o shell e os demais remotes continuam funcionando.
 */
class LimiteDeErro extends Component<{ nome: string; children: ReactNode }, { erro: Error | null }> {
  state = { erro: null as Error | null };

  static getDerivedStateFromError(erro: Error) {
    return { erro };
  }

  render() {
    if (this.state.erro) {
      return (
        <div className="card alerta" role="alert">
          O modulo <b>{this.props.nome}</b> esta indisponivel no momento. As demais areas continuam funcionando.
          <div><button className="btn btn-pequeno btn-secundario" onClick={() => this.setState({ erro: null })}>Tentar novamente</button></div>
        </div>
      );
    }
    return this.props.children;
  }
}

export function RemoteLoader<P extends object>({ nome, Componente, props }: { nome: string; Componente: ComponentType<P>; props: P }) {
  return (
    <LimiteDeErro nome={nome}>
      <Suspense fallback={<p className="texto-suave">Carregando {nome}...</p>}>
        <Componente {...props} />
      </Suspense>
    </LimiteDeErro>
  );
}
