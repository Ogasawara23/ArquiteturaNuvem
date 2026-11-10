package br.com.smartkds.producao.shared.persistencia;

import br.com.smartkds.producao.shared.dominio.IndicadoresOperacionais;
import br.com.smartkds.producao.shared.dominio.TenantId;

import java.time.Instant;

/** Consulta agregada por estacao e janela temporal (indice tenant_id, estacao_id, expedido_em). */
public interface IndicadoresRepository {

    IndicadoresOperacionais calcular(TenantId tenantId, Instant desde, String fusoHorario);
}
