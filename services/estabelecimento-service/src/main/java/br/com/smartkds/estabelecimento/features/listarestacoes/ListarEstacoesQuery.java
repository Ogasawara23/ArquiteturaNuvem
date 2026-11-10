package br.com.smartkds.estabelecimento.features.listarestacoes;

import br.com.smartkds.estabelecimento.shared.dominio.Estacao;
import br.com.smartkds.estabelecimento.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.estabelecimento.shared.dominio.TenantId;
import br.com.smartkds.estabelecimento.shared.persistencia.EstacaoRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ListarEstacoesQuery {

    private final EstacaoRepository estacoes;

    ListarEstacoesQuery(EstacaoRepository estacoes) {
        this.estacoes = estacoes;
    }

    public List<EstacaoDto> listar(TenantId tenantId) {
        return estacoes.listarEstacoes(tenantId).stream().map(ListarEstacoesQuery::dto).toList();
    }

    public EstacaoDto obter(TenantId tenantId, String codigo) {
        return estacoes.buscarPorCodigo(tenantId, Estacao.normalizarCodigo(codigo)).map(ListarEstacoesQuery::dto)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estacao", codigo));
    }

    private static EstacaoDto dto(Estacao e) {
        return new EstacaoDto(e.id(), e.codigo(), e.nome(), e.ordemExibicao(), e.percentualAtencao(), e.ativa());
    }
}
