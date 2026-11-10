package br.com.smartkds.pedido.shared.persistencia;

import br.com.smartkds.pedido.shared.dominio.RegistroAuditoria;
import br.com.smartkds.pedido.shared.dominio.TenantId;

import java.util.List;

public interface AuditoriaRepository {

    void salvarRegistro(RegistroAuditoria registro);

    List<RegistroAuditoria> listar(TenantId tenantId, int pagina, int tamanho);
}
