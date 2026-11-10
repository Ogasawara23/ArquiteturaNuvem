package br.com.smartkds.estabelecimento.features.configurarroteamento;

import br.com.smartkds.estabelecimento.shared.dominio.Estabelecimento;
import br.com.smartkds.estabelecimento.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.estabelecimento.shared.dominio.RegraDeRoteamento;
import br.com.smartkds.estabelecimento.shared.persistencia.EstabelecimentoRepository;
import br.com.smartkds.estabelecimento.shared.persistencia.EstacaoRepository;
import br.com.smartkds.estabelecimento.shared.persistencia.RegraRoteamentoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
class ConfigurarRoteamentoHandler {

    private final RegraRoteamentoRepository regras;
    private final EstacaoRepository estacoes;
    private final EstabelecimentoRepository estabelecimentos;

    ConfigurarRoteamentoHandler(RegraRoteamentoRepository regras, EstacaoRepository estacoes,
                                EstabelecimentoRepository estabelecimentos) {
        this.regras = regras;
        this.estacoes = estacoes;
        this.estabelecimentos = estabelecimentos;
    }

    @Transactional
    public void executar(ConfigurarRoteamentoCommand comando) {
        Estabelecimento atual = estabelecimentos.buscar(comando.tenantId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estabelecimento", comando.tenantId().valor()));
        var estacoesDoTenant = estacoes.listarEstacoes(comando.tenantId());
        List<RegraDeRoteamento> paraValidar = new ArrayList<>(comando.regras());
        RegraDeRoteamento.validarDestinos(paraValidar, estacoesDoTenant);

        Estabelecimento atualizado = new Estabelecimento(atual.tenantId(), atual.nomeFantasia(), atual.fusoHorario(),
                atual.percentualTaxaServico(), comando.estacaoPadrao(), atual.ativo());
        if (atualizado.estacaoPadrao() != null) {
            RegraDeRoteamento.validarDestinos(List.of(new RegraDeRoteamento(null, comando.tenantId(), null,
                    "padrao", atualizado.estacaoPadrao(), 0)), estacoesDoTenant);
        }
        estabelecimentos.salvar(atualizado);
        regras.substituirTodas(comando.tenantId(), comando.regras());
    }
}
