package br.com.smartkds.catalogo.features.publicarversaocardapio;

import br.com.smartkds.catalogo.features.RepositoriosEmMemoria;
import br.com.smartkds.catalogo.shared.dominio.DadosDoItem;
import br.com.smartkds.catalogo.shared.dominio.ItemCardapio;
import br.com.smartkds.catalogo.shared.dominio.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PublicarVersaoCardapioHandlerTest {

    private final TenantId tenant = new TenantId("rest-trattoria-centro");
    private final Clock relogio = Clock.fixed(Instant.parse("2026-10-15T15:40:00Z"), ZoneOffset.UTC);
    private final RepositoriosEmMemoria.Itens itens = new RepositoriosEmMemoria.Itens();
    private final RepositoriosEmMemoria.Versoes versoes = new RepositoriosEmMemoria.Versoes();
    private final PublicarVersaoCardapioHandler handler = new PublicarVersaoCardapioHandler(itens, versoes, relogio);

    @Test
    void publica_e_move_o_ponteiro_da_versao_ativa() {
        itens.salvar(ItemCardapio.cadastrar(tenant, new DadosDoItem("Batata frita", null, "Porcoes",
                2500, "fritura", 480, null, List.of()), relogio.instant()));

        var primeira = handler.executar(new PublicarVersaoCardapioCommand(tenant, "gerente"));
        var segunda = handler.executar(new PublicarVersaoCardapioCommand(tenant, "gerente"));

        assertThat(primeira.quantidadeDeItens()).isEqualTo(1);
        assertThat(versoes.dados).hasSize(2);
        assertThat(versoes.buscarAtiva(tenant).orElseThrow().versaoId()).isEqualTo(segunda.versaoId());
        assertThat(versoes.buscarPorId(tenant, primeira.versaoId())).isPresent();
    }
}
