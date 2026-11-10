package br.com.smartkds.estabelecimento.shared.persistencia;

import br.com.smartkds.estabelecimento.shared.dominio.Estabelecimento;
import br.com.smartkds.estabelecimento.shared.dominio.TenantId;

import java.util.Optional;

public interface EstabelecimentoRepository {

    Estabelecimento salvar(Estabelecimento estabelecimento);

    Optional<Estabelecimento> buscar(TenantId tenantId);
}
