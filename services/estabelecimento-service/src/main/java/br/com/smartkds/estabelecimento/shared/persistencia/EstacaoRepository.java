package br.com.smartkds.estabelecimento.shared.persistencia;

import br.com.smartkds.estabelecimento.shared.dominio.Estacao;
import br.com.smartkds.estabelecimento.shared.dominio.TenantId;

import java.util.List;
import java.util.Optional;

public interface EstacaoRepository {

    Estacao salvar(Estacao estacao);

    List<Estacao> listarEstacoes(TenantId tenantId);

    Optional<Estacao> buscarPorCodigo(TenantId tenantId, String codigo);
}
