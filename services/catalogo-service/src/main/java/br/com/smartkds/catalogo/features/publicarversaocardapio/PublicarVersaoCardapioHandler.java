package br.com.smartkds.catalogo.features.publicarversaocardapio;

import br.com.smartkds.catalogo.shared.dominio.VersaoCardapio;
import br.com.smartkds.catalogo.shared.persistencia.ItemCardapioRepository;
import br.com.smartkds.catalogo.shared.persistencia.VersaoCardapioRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Figura 20: le os itens do tenant, pede ao dominio a versao imutavel,
 * grava o documento fechado e move o ponteiro da versao ativa.
 * Comandas abertas continuam apontando para a versao anterior.
 */
@Component
class PublicarVersaoCardapioHandler {

    private final ItemCardapioRepository itens;
    private final VersaoCardapioRepository versoes;
    private final Clock relogio;

    PublicarVersaoCardapioHandler(ItemCardapioRepository itens, VersaoCardapioRepository versoes, Clock relogio) {
        this.itens = itens;
        this.versoes = versoes;
        this.relogio = relogio;
    }

    public PublicarVersaoCardapioResponse executar(PublicarVersaoCardapioCommand comando) {
        VersaoCardapio versao = VersaoCardapio.publicar(comando.tenantId(),
                itens.listar(comando.tenantId(), null), comando.publicadaPor(), Instant.now(relogio));
        versoes.salvar(versao);
        versoes.definirAtiva(comando.tenantId(), versao.versaoId());
        return new PublicarVersaoCardapioResponse(versao.versaoId(), versao.publicadaEm(), versao.quantidadeDeItens());
    }
}
