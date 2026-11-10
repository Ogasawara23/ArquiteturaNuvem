package br.com.smartkds.pedido.features.cancelarcomanda;

import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.ItemComanda;
import br.com.smartkds.pedido.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.pedido.shared.dominio.RegistroAuditoria;
import br.com.smartkds.pedido.shared.eventos.ItemCancelado;
import br.com.smartkds.pedido.shared.outbox.PublicadorDeEventos;
import br.com.smartkds.pedido.shared.persistencia.AuditoriaRepository;
import br.com.smartkds.pedido.shared.persistencia.ComandaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Delete do CRUD (exclusao logica, status CANCELADA). Para cada item que ja estava na cozinha
 * grava ItemCancelado na outbox: a producao compensa os tickets (mesma saga da Figura 19).
 */
@Component
class CancelarComandaHandler {

    private final ComandaRepository repositorio;
    private final PublicadorDeEventos publicador;
    private final AuditoriaRepository auditoria;
    private final Clock relogio;

    CancelarComandaHandler(ComandaRepository repositorio, PublicadorDeEventos publicador,
                           AuditoriaRepository auditoria, Clock relogio) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Transactional
    public void executar(CancelarComandaCommand c) {
        Comanda comanda = repositorio.buscarPorId(c.tenantId(), new ComandaId(c.comandaId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comanda", c.comandaId()));
        Instant agora = Instant.now(relogio);
        List<ItemComanda> naCozinha = comanda.cancelar(c.motivo(), agora);
        repositorio.salvar(comanda);
        naCozinha.forEach(item -> publicador.publicar(ItemCancelado.novo(comanda.tenantId().valor(),
                comanda.id().valor(), item.id().valor(), item.motivoCancelamento(), agora)));
        auditoria.salvarRegistro(RegistroAuditoria.de(c.tenantId(), c.usuarioId(), "COMANDA_CANCELADA",
                comanda.id().valor(), Map.of("mesa", comanda.mesaRotulo(), "itensCompensados", naCozinha.size(),
                        "motivo", c.motivo() == null ? "" : c.motivo()),
                c.correlationId(), agora));
    }
}
