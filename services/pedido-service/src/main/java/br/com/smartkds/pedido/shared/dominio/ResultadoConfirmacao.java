package br.com.smartkds.pedido.shared.dominio;

import java.util.List;

/**
 * Resultado de confirmarItensPendentes. {@code repeticao} indica que a mesma
 * chave de idempotencia ja havia sido usada: nada muda e nenhum evento novo e publicado.
 */
public record ResultadoConfirmacao(List<ItemComanda> itens, boolean repeticao) {
}
