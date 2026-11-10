package br.com.smartkds.pedido.features.abrircomanda;

import br.com.smartkds.pedido.shared.dominio.Comanda;
import br.com.smartkds.pedido.shared.integracao.CatalogoGateway;
import br.com.smartkds.pedido.shared.persistencia.ComandaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
class AbrirComandaHandler {

    private final ComandaRepository repositorio;
    private final CatalogoGateway catalogo;
    private final Clock relogio;

    AbrirComandaHandler(ComandaRepository repositorio, CatalogoGateway catalogo, Clock relogio) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
        this.relogio = relogio;
    }

    @Transactional
    public AbrirComandaResponse executar(AbrirComandaCommand comando) {
        var existente = repositorio.buscarAbertaPorMesa(comando.tenantId(), comando.mesaId());
        if (existente.isPresent()) {
            Comanda c = existente.get();
            return new AbrirComandaResponse(c.id().valor(), c.versaoCardapioId(), true, c.abertaEm());
        }
        String versaoAtiva = catalogo.versaoAtiva(comando.tenantId()).orElse(null);
        Comanda comanda = Comanda.abrir(comando.tenantId(), comando.mesaId(), comando.mesaRotulo(),
                versaoAtiva, Instant.now(relogio));
        repositorio.salvar(comanda);
        return new AbrirComandaResponse(comanda.id().valor(), comanda.versaoCardapioId(), false, comanda.abertaEm());
    }
}
