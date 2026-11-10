package br.com.smartkds.pedido.features.consultarauditoria;

import br.com.smartkds.pedido.shared.dominio.TenantId;
import br.com.smartkds.pedido.shared.persistencia.AuditoriaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class AuditoriaQuery {

    private final AuditoriaRepository auditoria;

    AuditoriaQuery(AuditoriaRepository auditoria) {
        this.auditoria = auditoria;
    }

    public List<RegistroAuditoriaDto> executar(TenantId tenantId, int pagina, int tamanho) {
        return auditoria.listar(tenantId, pagina, tamanho).stream()
                .map(r -> new RegistroAuditoriaDto(r.id(), r.usuarioId(), r.acao(), r.comandaId(), r.detalhe(),
                        r.correlationId(), r.ocorridoEm()))
                .toList();
    }
}
