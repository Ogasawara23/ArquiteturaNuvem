package br.com.smartkds.estabelecimento.shared.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Mapeamentos ORM do contexto. Invisiveis para as fatias. */
final class Entidades {

    private Entidades() { }

    @Entity
    @Table(name = "estabelecimento")
    static class EstabelecimentoEntity {
        @Id
        String tenantId;
        String nomeFantasia;
        String fusoHorario;
        BigDecimal percentualTaxaServico;
        String estacaoPadrao;
        boolean ativo;
    }

    @Entity
    @Table(name = "mesa")
    static class MesaEntity {
        @Id
        UUID id;
        String tenantId;
        String rotulo;
        String qrToken;
        boolean ativa;
    }

    @Entity
    @Table(name = "estacao")
    static class EstacaoEntity {
        @Id
        UUID id;
        String tenantId;
        String codigo;
        String nome;
        int ordemExibicao;
        int percentualAtencao;
        boolean ativa;
    }

    @Entity
    @Table(name = "regra_roteamento")
    static class RegraEntity {
        @Id
        UUID id;
        String tenantId;
        String categoriaCardapio;
        String itemCardapioId;
        String estacaoCodigo;
        int prioridade;
    }
}
