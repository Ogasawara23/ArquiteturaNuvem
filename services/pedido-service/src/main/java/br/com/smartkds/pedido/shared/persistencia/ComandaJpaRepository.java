package br.com.smartkds.pedido.shared.persistencia;

import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.Dinheiro;
import br.com.smartkds.pedido.shared.dominio.ItemComanda;
import br.com.smartkds.pedido.shared.dominio.ItemComandaId;
import br.com.smartkds.pedido.shared.dominio.MesaId;
import br.com.smartkds.pedido.shared.dominio.ModificadorSelecionado;
import br.com.smartkds.pedido.shared.dominio.ResumoDaComanda;
import br.com.smartkds.pedido.shared.dominio.StatusComanda;
import br.com.smartkds.pedido.shared.dominio.StatusItem;
import br.com.smartkds.pedido.shared.dominio.TenantId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Adaptador JPA: traduz entre o agregado (Java puro) e as entidades ORM. */
@Component
public class ComandaJpaRepository implements ComandaRepository {

    private final PedidoSpringData.Comandas comandas;

    ComandaJpaRepository(PedidoSpringData.Comandas comandas) {
        this.comandas = comandas;
    }

    @Override
    @Transactional
    public Comanda salvar(Comanda comanda) {
        comandas.save(paraEntidade(comanda));
        return comanda;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Comanda> buscarPorId(TenantId tenantId, ComandaId comandaId) {
        return comandas.findByTenantIdAndId(tenantId.valor(), comandaId.valor()).map(ComandaJpaRepository::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Comanda> buscarAbertaPorMesa(TenantId tenantId, MesaId mesaId) {
        return comandas.findFirstByTenantIdAndMesaIdAndStatus(tenantId.valor(), mesaId.valor(),
                StatusComanda.ABERTA.name()).map(ComandaJpaRepository::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comanda> listar(TenantId tenantId, StatusComanda statusOuNulo, MesaId mesaOuNulo) {
        List<ComandaEntity> entidades = statusOuNulo == null
                ? comandas.findTop200ByTenantIdOrderByAbertaEmDesc(tenantId.valor())
                : comandas.findTop200ByTenantIdAndStatusOrderByAbertaEmDesc(tenantId.valor(), statusOuNulo.name());
        return entidades.stream()
                .filter(e -> mesaOuNulo == null || e.mesaId.equals(mesaOuNulo.valor()))
                .map(ComandaJpaRepository::paraDominio)
                .toList();
    }

    private static ComandaEntity paraEntidade(Comanda c) {
        ComandaEntity e = new ComandaEntity();
        e.id = c.id().valor();
        e.tenantId = c.tenantId().valor();
        e.mesaId = c.mesaId().valor();
        e.mesaRotulo = c.mesaRotulo();
        e.versaoCardapioId = c.versaoCardapioId();
        e.status = c.status().name();
        e.abertaEm = c.abertaEm();
        e.fechadaEm = c.fechadaEm();
        e.versaoOtimista = c.versao();
        c.resumo().ifPresent(r -> {
            e.subtotalCentavos = r.subtotal().centavos();
            e.taxaServicoCentavos = r.taxaServico().centavos();
            e.totalCentavos = r.total().centavos();
        });
        for (ItemComanda i : c.itens()) {
            ItemComandaEntity ie = new ItemComandaEntity();
            ie.id = i.id().valor();
            ie.itemCardapioId = i.itemCardapioId();
            ie.descricao = i.descricao();
            ie.categoria = i.categoria();
            ie.precoUnitarioCentavos = i.precoUnitario().centavos();
            ie.quantidade = i.quantidade();
            ie.observacao = i.observacao();
            ie.estacaoSugerida = i.estacaoSugerida();
            ie.tempoAlvoPreparoSegundos = i.tempoAlvoPreparoSegundos();
            ie.status = i.status().name();
            ie.chaveConfirmacao = i.chaveConfirmacao();
            ie.motivoCancelamento = i.motivoCancelamento();
            ie.adicionadoEm = i.adicionadoEm();
            ie.confirmadoEm = i.confirmadoEm();
            ie.entregueEm = i.entregueEm();
            ie.canceladoEm = i.canceladoEm();
            i.modificadores().forEach(m -> ie.modificadores.add(
                    new ItemComandaEntity.Modificador(m.grupo(), m.opcao(), m.precoAdicional().centavos())));
            e.itens.add(ie);
        }
        return e;
    }

    private static Comanda paraDominio(ComandaEntity e) {
        List<ItemComanda> itens = e.itens.stream().map(ie -> new ItemComanda(
                new ItemComandaId(ie.id), ie.itemCardapioId, ie.descricao, ie.categoria,
                Dinheiro.deCentavos(ie.precoUnitarioCentavos), ie.quantidade,
                ie.modificadores.stream().map(m -> new ModificadorSelecionado(m.grupo, m.opcao,
                        Dinheiro.deCentavos(m.precoAdicionalCentavos))).toList(),
                ie.observacao, ie.estacaoSugerida, ie.tempoAlvoPreparoSegundos, StatusItem.valueOf(ie.status),
                ie.chaveConfirmacao, ie.motivoCancelamento, ie.adicionadoEm, ie.confirmadoEm, ie.entregueEm,
                ie.canceladoEm)).toList();
        ResumoDaComanda resumo = e.totalCentavos == null ? null : new ResumoDaComanda(
                Dinheiro.deCentavos(e.subtotalCentavos), Dinheiro.deCentavos(e.taxaServicoCentavos),
                Dinheiro.deCentavos(e.totalCentavos),
                (int) itens.stream().filter(i -> i.status() != StatusItem.CANCELADO).count());
        return Comanda.restaurar(new ComandaId(e.id), new TenantId(e.tenantId), new MesaId(e.mesaId),
                e.mesaRotulo, e.versaoCardapioId, StatusComanda.valueOf(e.status), itens, e.abertaEm,
                e.fechadaEm, resumo, e.versaoOtimista);
    }
}
