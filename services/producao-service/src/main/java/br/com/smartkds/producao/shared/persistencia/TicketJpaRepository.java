package br.com.smartkds.producao.shared.persistencia;

import br.com.smartkds.producao.shared.dominio.ItemTicket;
import br.com.smartkds.producao.shared.dominio.StatusTicket;
import br.com.smartkds.producao.shared.dominio.TenantId;
import br.com.smartkds.producao.shared.dominio.TicketProducao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TicketJpaRepository implements TicketRepository {

    private static final String SEPARADOR = "\n";
    private static final List<String> ATIVOS = Arrays.stream(StatusTicket.values())
            .filter(StatusTicket::ativo).map(Enum::name).toList();

    private final ProducaoSpringData.Tickets tickets;

    TicketJpaRepository(ProducaoSpringData.Tickets tickets) {
        this.tickets = tickets;
    }

    @Override
    @Transactional
    public void salvarTodos(List<TicketProducao> lista) {
        tickets.saveAll(lista.stream().map(TicketJpaRepository::paraEntidade).toList());
    }

    @Override
    @Transactional
    public TicketProducao salvar(TicketProducao ticket) {
        tickets.save(paraEntidade(ticket));
        return ticket;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TicketProducao> buscarPorId(TenantId tenantId, UUID ticketId) {
        return tickets.findByTenantIdAndId(tenantId.valor(), ticketId).map(TicketJpaRepository::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketProducao> listarAtivosPorEstacao(TenantId tenantId, String estacaoId) {
        return tickets.findByTenantIdAndEstacaoIdAndStatusInOrderByCriadoEmAsc(tenantId.valor(), estacaoId, ATIVOS)
                .stream().map(TicketJpaRepository::paraDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketProducao> buscarPorItemDaComanda(TenantId tenantId, UUID itemComandaId) {
        return tickets.buscarPorItemDaComanda(tenantId.valor(), itemComandaId).stream()
                .map(TicketJpaRepository::paraDominio).toList();
    }

    private static TicketEntity paraEntidade(TicketProducao t) {
        TicketEntity e = new TicketEntity();
        e.id = t.id();
        e.tenantId = t.tenantId().valor();
        e.comandaId = t.comandaId();
        e.mesaRotulo = t.mesaRotulo();
        e.estacaoId = t.estacaoId();
        e.status = t.status().name();
        e.tempoAlvoSegundos = t.tempoAlvoSegundos();
        e.criadoEm = t.criadoEm();
        e.iniciadoEm = t.iniciadoEm();
        e.prontoEm = t.prontoEm();
        e.expedidoEm = t.expedidoEm();
        e.canceladoEm = t.canceladoEm();
        e.versaoOtimista = t.versao();
        for (ItemTicket i : t.itens()) {
            TicketEntity.ItemTicketEntity ie = new TicketEntity.ItemTicketEntity();
            ie.id = i.id();
            ie.itemComandaId = i.itemComandaId();
            ie.descricao = i.descricao();
            ie.quantidade = i.quantidade();
            ie.modificadores = String.join(SEPARADOR, i.modificadores());
            ie.observacao = i.observacao();
            ie.cancelado = i.cancelado();
            e.itens.add(ie);
        }
        return e;
    }

    private static TicketProducao paraDominio(TicketEntity e) {
        List<ItemTicket> itens = e.itens.stream().map(ie -> new ItemTicket(ie.id, ie.itemComandaId, ie.descricao,
                ie.quantidade, ie.modificadores == null || ie.modificadores.isEmpty() ? List.of()
                        : List.of(ie.modificadores.split(SEPARADOR)),
                ie.observacao, ie.cancelado)).toList();
        return TicketProducao.restaurar(e.id, new TenantId(e.tenantId), e.comandaId, e.mesaRotulo, e.estacaoId,
                e.tempoAlvoSegundos, itens, StatusTicket.valueOf(e.status), e.criadoEm, e.iniciadoEm, e.prontoEm,
                e.expedidoEm, e.canceladoEm, e.versaoOtimista);
    }
}
