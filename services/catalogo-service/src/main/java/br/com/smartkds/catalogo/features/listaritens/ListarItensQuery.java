package br.com.smartkds.catalogo.features.listaritens;

import br.com.smartkds.catalogo.shared.dominio.TenantId;
import br.com.smartkds.catalogo.shared.persistencia.ItemCardapioRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class ListarItensQuery {

    private final ItemCardapioRepository repositorio;

    ListarItensQuery(ItemCardapioRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<ItemResumoDto> executar(TenantId tenantId, String categoria) {
        return repositorio.listar(tenantId, categoria).stream()
                .map(i -> new ItemResumoDto(i.id(), i.nome(), i.categoria(), i.precoCentavos(),
                        i.estacaoSugerida(), i.tempoAlvoPreparoSegundos(), i.disponivel(), i.atualizadoEm()))
                .toList();
    }
}
