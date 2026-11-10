package br.com.smartkds.producao.shared.dominio;

import java.time.Duration;

/** Classifica o tempo decorrido em relacao ao tempo-alvo: atencao acima de 70%, atraso acima de 100% (RF15). */
public record JanelaDeSla(int tempoAlvoSegundos, int percentualAtencao) {

    public static final int PERCENTUAL_ATENCAO_PADRAO = 70;

    public JanelaDeSla {
        if (tempoAlvoSegundos <= 0) {
            throw new RegraDeNegocioException("tempo-alvo deve ser positivo");
        }
        if (percentualAtencao <= 0 || percentualAtencao >= 100) {
            percentualAtencao = PERCENTUAL_ATENCAO_PADRAO;
        }
    }

    public static JanelaDeSla padrao(int tempoAlvoSegundos) {
        return new JanelaDeSla(tempoAlvoSegundos, PERCENTUAL_ATENCAO_PADRAO);
    }

    public SituacaoSla classificar(Duration decorrido) {
        long segundos = decorrido.toSeconds();
        if (segundos > tempoAlvoSegundos) {
            return SituacaoSla.ATRASADO;
        }
        if (segundos * 100 > (long) tempoAlvoSegundos * percentualAtencao) {
            return SituacaoSla.ATENCAO;
        }
        return SituacaoSla.NO_PRAZO;
    }
}
