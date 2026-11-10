package br.com.smartkds.pedido.features.confirmarpedido;

import br.com.smartkds.pedido.shared.web.RequisicaoInvalidaException;

class ConfirmarPedidoValidator {

    void validar(ConfirmarPedidoCommand comando) {
        if (comando.chaveIdempotencia() == null || comando.chaveIdempotencia().isBlank()) {
            throw new RequisicaoInvalidaException("chaveIdempotencia e obrigatoria");
        }
        if (comando.chaveIdempotencia().length() > 64) {
            throw new RequisicaoInvalidaException("chaveIdempotencia excede 64 caracteres");
        }
    }
}
