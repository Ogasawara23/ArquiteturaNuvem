package br.com.smartkds.pedido.features.confirmarpedido;

import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.dominio.ComandaId;
import br.com.smartkds.pedido.shared.dominio.RecursoNaoEncontradoException;
import br.com.smartkds.pedido.shared.dominio.ResultadoConfirmacao;
import br.com.smartkds.pedido.shared.outbox.PublicadorDeEventos;
import br.com.smartkds.pedido.shared.persistencia.ComandaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Orquestracao da funcionalidade. Nao decide nada sobre o dominio: quem decide se ha
 * itens pendentes e como transiciona-los e o agregado Comanda.
 */
@Component
class ConfirmarPedidoHandler {

    private final ComandaRepository repositorio;
    private final PublicadorDeEventos publicador;
    private final ConfirmarPedidoValidator validador;
    private final Clock relogio;

    ConfirmarPedidoHandler(ComandaRepository repositorio, PublicadorDeEventos publicador, Clock relogio) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.validador = new ConfirmarPedidoValidator();
        this.relogio = relogio;
    }

    @Transactional
    public ConfirmarPedidoResponse executar(ConfirmarPedidoCommand comando) {
        validador.validar(comando);

        Comanda comanda = repositorio
                .buscarPorId(comando.tenantId(), new ComandaId(comando.comandaId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comanda", comando.comandaId()));

        Instant agora = Instant.now(relogio);
        ResultadoConfirmacao resultado = comanda.confirmarItensPendentes(comando.chaveIdempotencia(), agora);

        if (!resultado.repeticao()) {
            repositorio.salvar(comanda);
            // Transactional Outbox: o evento e gravado na MESMA transacao do agregado.
            publicador.publicar(PedidoConfirmadoEventFactory.de(comanda, resultado.itens(),
                    comando.chaveIdempotencia(), agora));
        }

        return new ConfirmarPedidoResponse(
                comanda.id().valor(),
                resultado.itens().stream()
                        .map(item -> new ItemConfirmadoDto(item.id().valor(), item.descricao(), item.quantidade()))
                        .toList(),
                resultado.repeticao() ? resultado.itens().get(0).confirmadoEm() : agora,
                resultado.repeticao());
    }
}
