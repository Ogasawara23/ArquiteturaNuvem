package br.com.smartkds.estabelecimento.shared.dominio;

import java.math.BigDecimal;
import java.time.ZoneId;

/** Configuracao do tenant: nome, fuso, taxa de servico e estacao padrao de roteamento. */
public record Estabelecimento(TenantId tenantId,
                              String nomeFantasia,
                              String fusoHorario,
                              BigDecimal percentualTaxaServico,
                              String estacaoPadrao,
                              boolean ativo) {

    public Estabelecimento {
        if (nomeFantasia == null || nomeFantasia.isBlank()) {
            throw new RegraDeNegocioException("nomeFantasia e obrigatorio");
        }
        try {
            ZoneId.of(fusoHorario);
        } catch (RuntimeException e) {
            throw new RegraDeNegocioException("fuso horario invalido: " + fusoHorario);
        }
        if (percentualTaxaServico == null || percentualTaxaServico.signum() < 0
                || percentualTaxaServico.compareTo(BigDecimal.valueOf(30)) > 0) {
            throw new RegraDeNegocioException("taxa de servico deve estar entre 0 e 30%");
        }
        estacaoPadrao = estacaoPadrao == null || estacaoPadrao.isBlank() ? null : Estacao.normalizarCodigo(estacaoPadrao);
    }
}
