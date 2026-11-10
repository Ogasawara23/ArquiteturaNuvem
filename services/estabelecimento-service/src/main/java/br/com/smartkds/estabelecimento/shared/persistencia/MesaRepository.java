package br.com.smartkds.estabelecimento.shared.persistencia;

import br.com.smartkds.estabelecimento.shared.dominio.Mesa;
import br.com.smartkds.estabelecimento.shared.dominio.TenantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MesaRepository {

    Mesa salvar(Mesa mesa);

    List<Mesa> listar(TenantId tenantId);

    Optional<Mesa> buscarPorId(TenantId tenantId, UUID mesaId);
}
