package br.com.smartkds.producao.shared.integracao;

import br.com.smartkds.producao.shared.dominio.RegraRoteamento;
import br.com.smartkds.producao.shared.dominio.TenantId;

/**
 * Unica chamada sincrona entre microsservicos (Secao 4.5): leitura de configuracao,
 * protegida por cache local e por valor de contingencia. Nunca lanca excecao.
 */
public interface EstabelecimentoGateway {

    RegraRoteamento regraDeRoteamento(TenantId tenantId);
}
