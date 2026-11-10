package br.com.smartkds.pedido.features.consultarauditoria;

import br.com.smartkds.pedido.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Auditoria")
class ConsultarAuditoriaEndpoint {

    private final AuditoriaQuery query;

    ConsultarAuditoriaEndpoint(AuditoriaQuery query) {
        this.query = query;
    }

    @GetMapping("/interno/auditoria")
    @Operation(summary = "Trilha de auditoria paginada do estabelecimento (RF20)")
    public List<RegistroAuditoriaDto> listar(@RequestParam(defaultValue = "0") int pagina,
                                             @RequestParam(defaultValue = "50") int tamanho) {
        return query.executar(ContextoDoTenant.atual(), Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 200));
    }
}
