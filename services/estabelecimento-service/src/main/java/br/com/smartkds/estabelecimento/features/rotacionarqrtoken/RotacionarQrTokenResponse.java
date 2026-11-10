package br.com.smartkds.estabelecimento.features.rotacionarqrtoken;

import java.util.UUID;

record RotacionarQrTokenResponse(UUID mesaId, String rotulo, String qrToken) {
}
