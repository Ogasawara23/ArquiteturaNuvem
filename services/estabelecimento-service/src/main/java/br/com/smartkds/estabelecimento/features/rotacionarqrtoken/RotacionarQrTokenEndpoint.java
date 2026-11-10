package br.com.smartkds.estabelecimento.features.rotacionarqrtoken;

import br.com.smartkds.estabelecimento.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Mesas")
class RotacionarQrTokenEndpoint {

    private final RotacionarQrTokenHandler handler;

    RotacionarQrTokenEndpoint(RotacionarQrTokenHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/interno/mesas/{mesaId}/qr-token")
    @Operation(summary = "Gera novo qrToken e invalida o QR Code anterior (Secao 8.3)")
    public RotacionarQrTokenResponse rotacionar(@PathVariable UUID mesaId) {
        return handler.executar(new RotacionarQrTokenCommand(ContextoDoTenant.atual(), mesaId));
    }
}
