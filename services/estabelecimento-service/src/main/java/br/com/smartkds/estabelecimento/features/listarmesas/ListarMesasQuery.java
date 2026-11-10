package br.com.smartkds.estabelecimento.features.listarmesas;

import br.com.smartkds.estabelecimento.shared.dominio.TenantId;
import br.com.smartkds.estabelecimento.shared.persistencia.MesaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ListarMesasQuery {

    private final MesaRepository mesas;

    ListarMesasQuery(MesaRepository mesas) {
        this.mesas = mesas;
    }

    public List<MesaDto> executar(TenantId tenantId) {
        return mesas.listar(tenantId).stream().map(m -> new MesaDto(m.id(), m.rotulo(), m.qrToken(), m.ativa())).toList();
    }
}
