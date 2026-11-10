package br.com.smartkds.estabelecimento.features.consultarroteamento;

import br.com.smartkds.estabelecimento.shared.dominio.Estabelecimento;
import br.com.smartkds.estabelecimento.shared.dominio.TenantId;
import br.com.smartkds.estabelecimento.shared.persistencia.EstabelecimentoRepository;
import br.com.smartkds.estabelecimento.shared.persistencia.RegraRoteamentoRepository;
import org.springframework.stereotype.Component;

@Component
class RoteamentoQuery {

    private final RegraRoteamentoRepository regras;
    private final EstabelecimentoRepository estabelecimentos;

    RoteamentoQuery(RegraRoteamentoRepository regras, EstabelecimentoRepository estabelecimentos) {
        this.regras = regras;
        this.estabelecimentos = estabelecimentos;
    }

    public RoteamentoDto executar(TenantId tenantId) {
        String padrao = estabelecimentos.buscar(tenantId).map(Estabelecimento::estacaoPadrao).orElse(null);
        return new RoteamentoDto(padrao, regras.listarRegras(tenantId).stream()
                .map(r -> new RoteamentoDto.Regra(r.itemCardapioId(), r.categoria(), r.estacaoCodigo(), r.prioridade()))
                .toList());
    }
}
