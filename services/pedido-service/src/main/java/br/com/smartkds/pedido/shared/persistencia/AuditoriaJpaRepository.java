package br.com.smartkds.pedido.shared.persistencia;

import br.com.smartkds.pedido.shared.dominio.RegistroAuditoria;
import br.com.smartkds.pedido.shared.dominio.TenantId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class AuditoriaJpaRepository implements AuditoriaRepository {

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() { };

    private final PedidoSpringData.Auditoria auditoria;
    private final ObjectMapper json;

    AuditoriaJpaRepository(PedidoSpringData.Auditoria auditoria, ObjectMapper json) {
        this.auditoria = auditoria;
        this.json = json;
    }

    @Override
    @Transactional
    public void salvarRegistro(RegistroAuditoria r) {
        LogAuditoriaEntity e = new LogAuditoriaEntity();
        e.id = r.id();
        e.tenantId = r.tenantId().valor();
        e.usuarioId = r.usuarioId();
        e.acao = r.acao();
        e.comandaId = r.comandaId();
        e.correlationId = r.correlationId();
        e.ocorridoEm = r.ocorridoEm();
        try {
            e.detalhe = json.writeValueAsString(r.detalhe());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("detalhe de auditoria nao serializavel", ex);
        }
        auditoria.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroAuditoria> listar(TenantId tenantId, int pagina, int tamanho) {
        return auditoria.findByTenantIdOrderByOcorridoEmDesc(tenantId.valor(), PageRequest.of(pagina, tamanho))
                .stream()
                .map(e -> new RegistroAuditoria(e.id, new TenantId(e.tenantId), e.usuarioId, e.acao, e.comandaId,
                        lerDetalhe(e.detalhe), e.correlationId, e.ocorridoEm))
                .toList();
    }

    private Map<String, Object> lerDetalhe(String detalhe) {
        try {
            return detalhe == null ? Map.of() : json.readValue(detalhe, MAPA);
        } catch (JsonProcessingException ex) {
            return Map.of("bruto", detalhe);
        }
    }
}
