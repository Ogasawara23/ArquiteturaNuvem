package br.com.smartkds.catalogo.dominio;

import br.com.smartkds.catalogo.shared.dominio.DadosDoItem;
import br.com.smartkds.catalogo.shared.dominio.GrupoModificador;
import br.com.smartkds.catalogo.shared.dominio.ItemCardapio;
import br.com.smartkds.catalogo.shared.dominio.OpcaoModificador;
import br.com.smartkds.catalogo.shared.dominio.RegraDeNegocioException;
import br.com.smartkds.catalogo.shared.dominio.TenantId;
import br.com.smartkds.catalogo.shared.dominio.VersaoCardapio;
import br.com.smartkds.catalogo.shared.dominio.VersaoInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VersaoCardapioTest {

    private static final TenantId TENANT = new TenantId("rest-trattoria-centro");
    private static final Instant AGORA = Instant.parse("2026-10-15T15:40:00Z");

    static DadosDoItem hamburguer(long preco) {
        var ponto = new GrupoModificador("Ponto da carne", true, 1, 1, List.of(
                new OpcaoModificador("Ao ponto", 0, true),
                new OpcaoModificador("Bem passado", 0, true)));
        return new DadosDoItem("Hamburguer Artesanal 180g", "Pao brioche", "Lanches", preco, "Grill", 720,
                null, List.of(ponto));
    }

    @Test
    void publica_apenas_itens_disponiveis_do_tenant() {
        var disponivel = ItemCardapio.cadastrar(TENANT, hamburguer(4200), AGORA);
        var esgotado = ItemCardapio.cadastrar(TENANT, hamburguer(3900), AGORA);
        esgotado.alterarDisponibilidade(false, AGORA);
        var deOutroTenant = ItemCardapio.cadastrar(new TenantId("outro"), hamburguer(100), AGORA);

        var versao = VersaoCardapio.publicar(TENANT, List.of(disponivel, esgotado, deOutroTenant), "gerente", AGORA);

        assertThat(versao.itens()).hasSize(1);
        assertThat(versao.item(disponivel.id())).isPresent();
        assertThat(versao.item(esgotado.id())).isEmpty();
    }

    @Test
    void versao_publicada_nao_muda_quando_o_item_muda() {
        var item = ItemCardapio.cadastrar(TENANT, hamburguer(4200), AGORA);
        var versao = VersaoCardapio.publicar(TENANT, List.of(item), "gerente", AGORA);

        item.atualizar(hamburguer(9900), AGORA.plusSeconds(60));

        assertThat(versao.item(item.id()).orElseThrow().precoCentavos()).isEqualTo(4200);
    }

    @Test
    void publicar_sem_itens_disponiveis_e_invalido() {
        assertThatThrownBy(() -> VersaoCardapio.publicar(TENANT, List.of(), "gerente", AGORA))
                .isInstanceOf(VersaoInvalidaException.class);
    }

    @Test
    void item_exige_preco_positivo_e_normaliza_estacao() {
        assertThatThrownBy(() -> ItemCardapio.cadastrar(TENANT, hamburguer(0), AGORA))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThat(ItemCardapio.cadastrar(TENANT, hamburguer(10), AGORA).estacaoSugerida()).isEqualTo("grill");
    }

    @Test
    void grupo_obrigatorio_exige_ao_menos_uma_selecao() {
        assertThatThrownBy(() -> new GrupoModificador("Ponto", true, 0, 1,
                List.of(new OpcaoModificador("Ao ponto", 0, true))))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
