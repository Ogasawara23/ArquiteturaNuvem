package br.com.smartkds.pedido.features.cancelaritem;

import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.ItemComanda;
import br.com.smartkds.pedido.shared.dominio.ItemComandaId;
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
import java.util.Map;

/**
 * Inicio da saga coreografada (Figura 19). Se o item ja estava na cozinha, grava
 * ItemCancelado na outbox para que o producao-service compense o ticket.
 */
@Component
class CancelarItemHandler {

    private final ComandaRepository repositorio;
    private final PublicadorDeEventos publicador;
    private final AuditoriaRepository auditoria;
    private final Clock relogio;

    CancelarItemHandler(ComandaRepository repositorio, PublicadorDeEventos publicador,
                        AuditoriaRepository auditoria, Clock relogio) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Transactional
    public void executar(CancelarItemCommand comando) {
        Comanda comanda = repositorio.buscarPorId(comando.tenantId(), new ComandaId(comando.comandaId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comanda", comando.comandaId()));
        Instant agora = Instant.now(relogio);

        ItemComanda item = comanda.cancelarItem(new ItemComandaId(comando.itemId()), comando.motivo(), agora);
        repositorio.salvar(comanda);

        if (item.foiEnviadoParaCozinha()) {
            publicador.publicar(ItemCancelado.novo(comanda.tenantId().valor(), comanda.id().valor(),
                    item.id().valor(), item.motivoCancelamento(), agora));
        }
        auditoria.salvarRegistro(RegistroAuditoria.de(comando.tenantId(), comando.usuarioId(), "ITEM_CANCELADO",
                comanda.id().valor(), Map.of(
                        "itemId", item.id().valor().toString(),
                        "descricao", item.descricao(),
                        "motivo", item.motivoCancelamento(),
                        "enviadoParaCozinha", item.foiEnviadoParaCozinha()),
                comando.correlationId(), agora));
    }
}
