package br.com.smartkds.estabelecimento.shared.dominio;

import java.text.Normalizer;
import java.util.UUID;

/** Posto de producao da cozinha. O codigo e o mesmo valor usado em estacaoSugerida no catalogo. */
public record Estacao(UUID id,
                      TenantId tenantId,
                      String codigo,
                      String nome,
                      int ordemExibicao,
                      int percentualAtencao,
                      boolean ativa) {

    public Estacao {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("nome da estacao e obrigatorio");
        }
        codigo = normalizarCodigo(codigo == null || codigo.isBlank() ? nome : codigo);
        if (percentualAtencao <= 0 || percentualAtencao >= 100) {
            throw new RegraDeNegocioException("percentualAtencao deve estar entre 1 e 99");
        }
    }

    public static Estacao cadastrar(TenantId tenantId, String codigo, String nome, int ordem, int percentualAtencao) {
        return new Estacao(UUID.randomUUID(), tenantId, codigo, nome, ordem, percentualAtencao, true);
    }

    /** "Sobremesa Fria" vira "sobremesa-fria". */
    public static String normalizarCodigo(String valor) {
        String semAcento = Normalizer.normalize(valor.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
