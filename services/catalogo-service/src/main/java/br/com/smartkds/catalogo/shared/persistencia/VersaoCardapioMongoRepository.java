package br.com.smartkds.catalogo.shared.persistencia;

import br.com.smartkds.catalogo.shared.dominio.TenantId;
import br.com.smartkds.catalogo.shared.dominio.VersaoCardapio;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Adaptador MongoDB da porta {@link VersaoCardapioRepository}. */
@Component
public class VersaoCardapioMongoRepository implements VersaoCardapioRepository {

    private final CatalogoSpringData.Versoes versoes;
    private final CatalogoSpringData.Ponteiros ponteiros;

    VersaoCardapioMongoRepository(CatalogoSpringData.Versoes versoes, CatalogoSpringData.Ponteiros ponteiros) {
        this.versoes = versoes;
        this.ponteiros = ponteiros;
    }

    @Override
    public VersaoCardapio salvar(VersaoCardapio v) {
        versoes.insert(new VersaoDocumento(v.versaoId(), v.tenantId().valor(), v.publicadaEm(),
                v.publicadaPor(), v.quantidadeDeItens(), v.itens()));
        return v;
    }

    @Override
    public Optional<VersaoCardapio> buscarPorId(TenantId tenantId, String versaoId) {
        return versoes.findByTenantIdAndVersaoId(tenantId.valor(), versaoId)
                .map(VersaoCardapioMongoRepository::paraDominio);
    }

    @Override
    public Optional<VersaoCardapio> buscarAtiva(TenantId tenantId) {
        return ponteiros.findById(tenantId.valor())
                .flatMap(p -> buscarPorId(tenantId, p.versaoAtivaId()));
    }

    @Override
    public void definirAtiva(TenantId tenantId, String versaoId) {
        ponteiros.save(new PonteiroVersaoAtivaDocumento(tenantId.valor(), versaoId));
    }

    private static VersaoCardapio paraDominio(VersaoDocumento d) {
        return VersaoCardapio.restaurar(d.versaoId(), new TenantId(d.tenantId()), d.publicadaEm(),
                d.publicadaPor(), d.itens());
    }
}
