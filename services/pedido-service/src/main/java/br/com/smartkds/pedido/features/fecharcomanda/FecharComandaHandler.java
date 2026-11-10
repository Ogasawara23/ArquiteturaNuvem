package br.com.smartkds.pedido.features.fecharcomanda;

import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.pedido.shared.dominio.RegistroAuditoria;
import br.com.smartkds.pedido.shared.dominio.ResumoDaComanda;
import br.com.smartkds.pedido.shared.eventos.ComandaFechada;
import br.com.smartkds.pedido.shared.outbox.PublicadorDeEventos;
import br.com.smartkds.pedido.shared.persistencia.AuditoriaRepository;
import br.com.smartkds.pedido.shared.persistencia.ComandaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@Component
class FecharComandaHandler {

    private final ComandaRepository repositorio;
    private final PublicadorDeEventos publicador;
    private final AuditoriaRepository auditoria;
    private final Clock relogio;

    FecharComandaHandler(ComandaRepository repositorio, PublicadorDeEventos publicador,
                         AuditoriaRepository auditoria, Clock relogio) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Transactional
    public FecharComandaResponse executar(FecharComandaCommand comando) {
        Comanda comanda = repositorio.buscarPorId(comando.tenantId(), new ComandaId(comando.comandaId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comanda", comando.comandaId()));
        Instant agora = Instant.now(relogio);

        ResumoDaComanda resumo = comanda.fechar(comando.percentualTaxaServico(), agora);
        repositorio.salvar(comanda);

        publicador.publicar(ComandaFechada.novo(comanda.tenantId().valor(), comanda.id().valor(),
                comanda.mesaId().valor(), comanda.mesaRotulo(), resumo.subtotal().centavos(),
                resumo.taxaServico().centavos(), resumo.total().centavos(), resumo.quantidadeDeItens(), agora));
        auditoria.salvarRegistro(RegistroAuditoria.de(comando.tenantId(), comando.usuarioId(), "COMANDA_FECHADA",
                comanda.id().valor(), Map.of(
                        "totalCentavos", resumo.total().centavos(),
                        "percentualTaxaServico", comando.percentualTaxaServico().toPlainString()),
                comando.correlationId(), agora));

        return new FecharComandaResponse(comanda.id().valor(), comanda.mesaRotulo(), resumo.subtotal().centavos(),
                resumo.taxaServico().centavos(), resumo.total().centavos(), resumo.quantidadeDeItens(), agora);
    }
}
