package br.com.smartkds.producao.shared.persistencia;

import br.com.smartkds.producao.shared.dominio.IndicadoresOperacionais;
import br.com.smartkds.producao.shared.dominio.TenantId;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** Consulta agregada em SQL: o PostgreSQL calcula medias e contagens por estacao e por hora. */
@Component
public class IndicadoresJdbcRepository implements IndicadoresRepository {

    private static final String POR_ESTACAO = """
            SELECT estacao_id,
                   count(*)                                                         AS criados,
                   count(*) FILTER (WHERE status = 'EXPEDIDO')                      AS expedidos,
                   count(*) FILTER (WHERE status IN ('RECEBIDO','EM_PREPARO','PRONTO')) AS ativos,
                   count(*) FILTER (WHERE pronto_em IS NOT NULL
                                    AND extract(epoch FROM pronto_em - criado_em) > tempo_alvo_segundos) AS atrasados,
                   coalesce(avg(extract(epoch FROM pronto_em - criado_em))
                            FILTER (WHERE pronto_em IS NOT NULL), 0)                AS tempo_medio
              FROM ticket_producao
             WHERE tenant_id = :tenant AND criado_em >= :desde
             GROUP BY estacao_id
             ORDER BY estacao_id
            """;

    private static final String POR_HORA = """
            SELECT CAST(extract(hour FROM criado_em AT TIME ZONE :fuso) AS int) AS hora, count(*) AS tickets
              FROM ticket_producao
             WHERE tenant_id = :tenant AND criado_em >= :desde
             GROUP BY 1
             ORDER BY 1
            """;

    private final NamedParameterJdbcTemplate jdbc;

    IndicadoresJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public IndicadoresOperacionais calcular(TenantId tenantId, Instant desde, String fusoHorario) {
        var parametros = new MapSqlParameterSource()
                .addValue("tenant", tenantId.valor())
                .addValue("desde", Timestamp.from(desde))
                .addValue("fuso", fusoHorario);
        List<IndicadoresOperacionais.PorEstacao> estacoes = jdbc.query(POR_ESTACAO, parametros, (rs, i) ->
                new IndicadoresOperacionais.PorEstacao(rs.getString("estacao_id"), rs.getLong("criados"),
                        rs.getLong("expedidos"), rs.getLong("ativos"), rs.getLong("atrasados"),
                        rs.getDouble("tempo_medio")));
        List<IndicadoresOperacionais.PorHora> porHora = jdbc.query(POR_HORA, parametros, (rs, i) ->
                new IndicadoresOperacionais.PorHora(rs.getInt("hora"), rs.getLong("tickets")));
        return new IndicadoresOperacionais(desde, estacoes, porHora);
    }
}
