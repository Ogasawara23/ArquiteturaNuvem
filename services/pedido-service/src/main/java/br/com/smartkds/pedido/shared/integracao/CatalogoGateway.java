package br.com.smartkds.pedido.shared.integracao;

import br.com.smartkds.pedido.shared.dominio.SnapshotItemCardapio;
import br.com.smartkds.pedido.shared.dominio.TenantId;

import java.util.Optional;

/** Unico ponto do pedido-service que conhece outro servico (leitura sincrona, sem alterar estado). */
public interface CatalogoGateway {

    /** Versao ativa do cardapio do tenant; vazio se nunca houve publicacao. */
    Optional<String> versaoAtiva(TenantId tenantId);

    /** Snapshot do item na versao informada; vazio se o item nao existe nela. */
    Optional<SnapshotItemCardapio> obterItemDaVersao(TenantId tenantId, String versaoCardapioId, String itemCardapioId);
}
