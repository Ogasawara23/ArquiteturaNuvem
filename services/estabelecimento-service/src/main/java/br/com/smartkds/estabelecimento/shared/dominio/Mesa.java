package br.com.smartkds.estabelecimento.shared.dominio;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/** Mesa identificada por QR Code. O qrToken e a credencial de escopo limitado do cliente (Secao 8.3). */
public class Mesa {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UUID id;
    private final TenantId tenantId;
    private final String rotulo;
    private String qrToken;
    private boolean ativa;

    public Mesa(UUID id, TenantId tenantId, String rotulo, String qrToken, boolean ativa) {
        if (rotulo == null || rotulo.isBlank()) {
            throw new RegraDeNegocioException("rotulo da mesa e obrigatorio");
        }
        this.id = id;
        this.tenantId = tenantId;
        this.rotulo = rotulo.trim();
        this.qrToken = qrToken;
        this.ativa = ativa;
    }

    public static Mesa cadastrar(TenantId tenantId, String rotulo) {
        return new Mesa(UUID.randomUUID(), tenantId, rotulo, novoToken(), true);
    }

    /** Invalida o QR Code impresso anterior (por exemplo, apos vazamento). */
    public void rotacionarQrToken() {
        this.qrToken = novoToken();
    }

    public void desativar() {
        this.ativa = false;
    }

    /** 192 bits de entropia, URL-safe. */
    static String novoToken() {
        byte[] bytes = new byte[24];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public UUID id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public String rotulo() { return rotulo; }
    public String qrToken() { return qrToken; }
    public boolean ativa() { return ativa; }
}
