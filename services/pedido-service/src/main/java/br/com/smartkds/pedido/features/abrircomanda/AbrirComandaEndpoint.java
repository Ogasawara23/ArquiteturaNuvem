package br.com.smartkds.pedido.features.abrircomanda;

import br.com.smartkds.pedido.shared.dominio.MesaId;
import br.com.smartkds.pedido.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Comandas")
class AbrirComandaEndpoint {

    private final AbrirComandaHandler handler;

    AbrirComandaEndpoint(AbrirComandaHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/interno/comandas")
    @Operation(summary = "Abre a comanda da mesa ou reaproveita a que ja esta aberta (RF05)")
    public ResponseEntity<AbrirComandaResponse> abrir(@Valid @RequestBody AbrirComandaRequest r) {
        AbrirComandaResponse resposta = handler.executar(
                new AbrirComandaCommand(ContextoDoTenant.atual(), new MesaId(r.mesaId()), r.mesaRotulo()));
        return ResponseEntity.status(resposta.reaproveitada() ? HttpStatus.OK : HttpStatus.CREATED).body(resposta);
    }
}
