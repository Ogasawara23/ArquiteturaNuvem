package br.com.smartkds.pedido.shared.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Valor monetario em centavos (BRL). Nunca usa ponto flutuante. */
public record Dinheiro(long centavos) {

    public static Dinheiro zero() {
        return new Dinheiro(0);
    }

    public static Dinheiro deCentavos(long centavos) {
        return new Dinheiro(centavos);
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(Math.addExact(centavos, outro.centavos));
    }

    public Dinheiro multiplicar(int fator) {
        return new Dinheiro(Math.multiplyExact(centavos, fator));
    }

    /** Percentual (ex.: 10 para 10%), arredondado com HALF_EVEN. */
    public Dinheiro percentual(BigDecimal taxa) {
        if (taxa == null || taxa.signum() < 0) {
            throw new RegraDeNegocioException("percentual invalido");
        }
        long valor = BigDecimal.valueOf(centavos).multiply(taxa)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_EVEN)
                .longValueExact();
        return new Dinheiro(valor);
    }
}
