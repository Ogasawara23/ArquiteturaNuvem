package br.com.smartkds.estabelecimento.features.cadastrarmesa;

import br.com.smartkds.estabelecimento.shared.dominio.ConflitoException;
import br.com.smartkds.estabelecimento.shared.dominio.Mesa;
import br.com.smartkds.estabelecimento.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.estabelecimento.shared.persistencia.EstabelecimentoRepository;
import br.com.smartkds.estabelecimento.shared.persistencia.MesaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class CadastrarMesaHandler {

    private final MesaRepository mesas;
    private final EstabelecimentoRepository estabelecimentos;

    CadastrarMesaHandler(MesaRepository mesas, EstabelecimentoRepository estabelecimentos) {
        this.mesas = mesas;
        this.estabelecimentos = estabelecimentos;
    }

    @Transactional
    public CadastrarMesaResponse executar(CadastrarMesaCommand comando) {
        estabelecimentos.buscar(comando.tenantId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estabelecimento", comando.tenantId().valor()));
        if (mesas.listar(comando.tenantId()).stream().anyMatch(m -> m.rotulo().equalsIgnoreCase(comando.rotulo().trim()))) {
            throw new ConflitoException("ja existe mesa com o rotulo " + comando.rotulo());
        }
        Mesa mesa = Mesa.cadastrar(comando.tenantId(), comando.rotulo());
        mesas.salvar(mesa);
        return new CadastrarMesaResponse(mesa.id(), mesa.rotulo(), mesa.qrToken());
    }
}
