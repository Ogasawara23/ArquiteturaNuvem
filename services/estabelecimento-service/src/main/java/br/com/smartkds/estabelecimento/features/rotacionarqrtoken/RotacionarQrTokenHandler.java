package br.com.smartkds.estabelecimento.features.rotacionarqrtoken;

import br.com.smartkds.estabelecimento.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.estabelecimento.shared.persistencia.MesaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class RotacionarQrTokenHandler {

    private final MesaRepository mesas;

    RotacionarQrTokenHandler(MesaRepository mesas) {
        this.mesas = mesas;
    }

    @Transactional
    public RotacionarQrTokenResponse executar(RotacionarQrTokenCommand comando) {
        var mesa = mesas.buscarPorId(comando.tenantId(), comando.mesaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mesa", comando.mesaId()));
        mesa.rotacionarQrToken();
        mesas.salvar(mesa);
        return new RotacionarQrTokenResponse(mesa.id(), mesa.rotulo(), mesa.qrToken());
    }
}
