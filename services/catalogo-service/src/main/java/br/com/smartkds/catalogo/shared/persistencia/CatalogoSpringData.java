package br.com.smartkds.catalogo.shared.persistencia;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Interfaces do Spring Data, visiveis apenas dentro de shared.persistencia.
 * As fatias nunca as enxergam: elas usam as portas *Repository.
 */
final class CatalogoSpringData {

    private CatalogoSpringData() { }

    interface Itens extends MongoRepository<ItemDocumento, String> {
        Optional<ItemDocumento> findByTenantIdAndId(String tenantId, String id);

        List<ItemDocumento> findByTenantIdOrderByCategoriaAscNomeAsc(String tenantId);

        List<ItemDocumento> findByTenantIdAndCategoriaOrderByNomeAsc(String tenantId, String categoria);

        long deleteByTenantIdAndId(String tenantId, String id);
    }

    interface Versoes extends MongoRepository<VersaoDocumento, String> {
        Optional<VersaoDocumento> findByTenantIdAndVersaoId(String tenantId, String versaoId);
    }

    interface Ponteiros extends MongoRepository<PonteiroVersaoAtivaDocumento, String> {
    }
}
