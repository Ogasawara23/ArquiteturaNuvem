package br.com.smartkds.estabelecimento.features.listarmesas;

import java.util.UUID;

record MesaDto(UUID mesaId, String rotulo, String qrToken, boolean ativa) {
}
