package br.com.smartkds.estabelecimento.features.configurarestabelecimento;

import br.com.smartkds.estabelecimento.shared.dominio.Estabelecimento;
import br.com.smartkds.estabelecimento.shared.persistencia.EstabelecimentoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class ConfigurarEstabelecimentoHandler {

    private final EstabelecimentoRepository repositorio;

    ConfigurarEstabelecimentoHandler(EstabelecimentoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public Estabelecimento executar(Estabelecimento estabelecimento) {
        return repositorio.salvar(estabelecimento);
    }
}
