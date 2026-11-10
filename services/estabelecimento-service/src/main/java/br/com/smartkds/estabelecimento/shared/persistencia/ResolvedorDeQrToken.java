package br.com.smartkds.estabelecimento.shared.persistencia;

import br.com.smartkds.estabelecimento.shared.dominio.Mesa;

import java.util.Optional;

/** Unica leitura sem tenant: e ela que descobre o par (tenantId, mesaId) a partir do QR Code. */
public interface ResolvedorDeQrToken {

    Optional<Mesa> resolver(String qrToken);
}
