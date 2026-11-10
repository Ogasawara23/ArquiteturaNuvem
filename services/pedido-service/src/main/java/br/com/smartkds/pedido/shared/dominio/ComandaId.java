package br.com.smartkds.pedido.shared.dominio;

import java.util.Objects;
import java.util.UUID;

public record ComandaId(UUID valor) {
    public ComandaId {
        Objects.requireNonNull(valor, "comandaId");
    }

    public static ComandaId novo() {
        return new ComandaId(UUID.randomUUID());
    }
}
