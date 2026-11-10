package br.com.smartkds.estabelecimento.features.cadastrarestacao;

import br.com.smartkds.estabelecimento.shared.dominio.ConflitoException;
import br.com.smartkds.estabelecimento.shared.dominio.Estacao;
import br.com.smartkds.estabelecimento.shared.persistencia.EstacaoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class CadastrarEstacaoHandler {

    private final EstacaoRepository estacoes;

    CadastrarEstacaoHandler(EstacaoRepository estacoes) {
        this.estacoes = estacoes;
    }

    @Transactional
    public CadastrarEstacaoResponse executar(CadastrarEstacaoCommand c) {
        Estacao estacao = Estacao.cadastrar(c.tenantId(), c.codigo(), c.nome(), c.ordemExibicao(), c.percentualAtencao());
        if (estacoes.buscarPorCodigo(c.tenantId(), estacao.codigo()).isPresent()) {
            throw new ConflitoException("ja existe estacao com o codigo " + estacao.codigo());
        }
        estacoes.salvar(estacao);
        return new CadastrarEstacaoResponse(estacao.id(), estacao.codigo(), estacao.nome());
    }
}
