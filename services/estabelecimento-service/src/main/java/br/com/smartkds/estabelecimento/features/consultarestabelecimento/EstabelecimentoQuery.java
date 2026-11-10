package br.com.smartkds.estabelecimento.features.consultarestabelecimento;

import br.com.smartkds.estabelecimento.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.estabelecimento.shared.dominio.TenantId;
import br.com.smartkds.estabelecimento.shared.persistencia.EstabelecimentoRepository;
import br.com.smartkds.estabelecimento.shared.persistencia.EstacaoRepository;
import br.com.smartkds.estabelecimento.shared.persistencia.MesaRepository;
import org.springframework.stereotype.Component;

@Component
class EstabelecimentoQuery {

    private final EstabelecimentoRepository estabelecimentos;
    private final MesaRepository mesas;
    private final EstacaoRepository estacoes;

    EstabelecimentoQuery(EstabelecimentoRepository estabelecimentos, MesaRepository mesas, EstacaoRepository estacoes) {
        this.estabelecimentos = estabelecimentos;
        this.mesas = mesas;
        this.estacoes = estacoes;
    }

    public EstabelecimentoDto executar(TenantId tenantId) {
        var e = estabelecimentos.buscar(tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estabelecimento", tenantId.valor()));
        return new EstabelecimentoDto(e.tenantId().valor(), e.nomeFantasia(), e.fusoHorario(),
                e.percentualTaxaServico(), e.estacaoPadrao(), e.ativo(), mesas.listar(tenantId).size(),
                estacoes.listarEstacoes(tenantId).size());
    }
}
