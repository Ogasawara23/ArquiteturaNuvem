package br.com.smartkds.catalogo.shared.persistencia;

import br.com.smartkds.catalogo.shared.dominio.TenantId;
import br.com.smartkds.catalogo.shared.dominio.VersaoCardapio;

import java.util.Optional;

public interface VersaoCardapioRepository {

    VersaoCardapio salvar(VersaoCardapio versao);

    Optional<VersaoCardapio> buscarPorId(TenantId tenantId, String versaoId);

    Optional<VersaoCardapio> buscarAtiva(TenantId tenantId);

    void definirAtiva(TenantId tenantId, String versaoId);
}
