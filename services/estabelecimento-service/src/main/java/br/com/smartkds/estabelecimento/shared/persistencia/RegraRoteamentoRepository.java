package br.com.smartkds.estabelecimento.shared.persistencia;

import br.com.smartkds.estabelecimento.shared.dominio.RegraDeRoteamento;
import br.com.smartkds.estabelecimento.shared.dominio.TenantId;

import java.util.List;

public interface RegraRoteamentoRepository {

    /** Substitui o conjunto de regras do tenant em uma unica transacao (PUT idempotente). */
    void substituirTodas(TenantId tenantId, List<RegraDeRoteamento> regras);

    List<RegraDeRoteamento> listarRegras(TenantId tenantId);
}
