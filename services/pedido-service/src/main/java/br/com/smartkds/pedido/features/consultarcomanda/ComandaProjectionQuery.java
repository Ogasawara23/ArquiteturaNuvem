package br.com.smartkds.pedido.features.consultarcomanda;

import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.MesaId;
import br.com.smartkds.pedido.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.pedido.shared.dominio.StatusComanda;
import br.com.smartkds.pedido.shared.dominio.TenantId;
import br.com.smartkds.pedido.shared.persistencia.ComandaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** Projecao de leitura: monta o DTO da tela, sem passar por nenhuma regra de escrita. */
@Component
class ComandaProjectionQuery {

    private final ComandaRepository repositorio;

    ComandaProjectionQuery(ComandaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public ComandaDto porId(TenantId tenantId, ComandaId comandaId) {
        return repositorio.buscarPorId(tenantId, comandaId)
                .map(ComandaProjectionQuery::projetar)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comanda", comandaId.valor()));
    }

    public List<ComandaDto> listar(TenantId tenantId, StatusComanda status, MesaId mesaId) {
        return repositorio.listar(tenantId, status, mesaId).stream().map(ComandaProjectionQuery::projetar).toList();
    }

    private static ComandaDto projetar(Comanda c) {
        var itens = c.itens().stream().map(i -> new ComandaDto.ItemDto(
                i.id().valor(), i.itemCardapioId(), i.descricao(), i.quantidade(),
                i.precoUnitario().centavos(), i.total().centavos(), i.modificadoresComoTexto(),
                i.observacao(), i.estacaoSugerida(), i.status().name(), i.podeSerCancelado(),
                i.adicionadoEm(), i.confirmadoEm(), i.entregueEm())).toList();
        var resumo = c.resumo();
        return new ComandaDto(c.id().valor(), c.mesaId().valor(), c.mesaRotulo(), c.status().name(),
                c.versaoCardapioId(), c.subtotal().centavos(),
                resumo.map(r -> r.taxaServico().centavos()).orElse(null),
                resumo.map(r -> r.total().centavos()).orElse(null),
                c.abertaEm(), c.fechadaEm(), itens);
    }
}
