package br.com.smartkds.estabelecimento.features.cadastrarmesa;

import java.util.UUID;

record CadastrarMesaResponse(UUID mesaId, String rotulo, String qrToken) {
}
