package br.com.smartkds.pedido.features.confirmarpedido;

import br.com.smartkds.pedido.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Ponto de entrada HTTP: traduz HTTP em comando, sem regra de negocio (Secao 5.3.8). */
@RestController
@Tag(name = "Comandas")
class ConfirmarPedidoEndpoint {

    private final ConfirmarPedidoHandler handler;

    ConfirmarPedidoEndpoint(ConfirmarPedidoHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/interno/comandas/{comandaId}/confirmacao")
    @Operation(summary = "Envia os itens pendentes para a cozinha e publica PedidoConfirmado via outbox (RF07)")
    public ConfirmarPedidoResponse confirmar(@PathVariable UUID comandaId,
                                             @RequestBody ConfirmarPedidoRequest requisicao) {
        var comando = new ConfirmarPedidoCommand(ContextoDoTenant.atual(), comandaId, requisicao.chaveIdempotencia());
        return handler.executar(comando);
    }
}
