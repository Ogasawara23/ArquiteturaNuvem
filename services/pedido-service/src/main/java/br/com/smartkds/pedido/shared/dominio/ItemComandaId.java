package br.com.smartkds.pedido.shared.dominio;

import java.util.Objects;
import java.util.UUID;

public record ItemComandaId(UUID valor) {
    public ItemComandaId {
        Objects.requireNonNull(valor, "itemComandaId");
    }

    public static ItemComandaId novo() {
        return new ItemComandaId(UUID.randomUUID());
    }
}
