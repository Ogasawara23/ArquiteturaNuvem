package br.com.smartkds.estabelecimento.dominio;

import br.com.smartkds.estabelecimento.shared.dominio.Estabelecimento;
import br.com.smartkds.estabelecimento.shared.dominio.Estacao;
import br.com.smartkds.estabelecimento.shared.dominio.Mesa;
import br.com.smartkds.estabelecimento.shared.dominio.RegraDeNegocioException;
import br.com.smartkds.estabelecimento.shared.dominio.RegraDeRoteamento;
import br.com.smartkds.estabelecimento.shared.dominio.TenantId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DominioEstabelecimentoTest {

    private static final TenantId T = new TenantId("demo");

    @Test
    void qr_token_tem_alta_entropia_e_muda_na_rotacao() {
        Mesa mesa = Mesa.cadastrar(T, "Mesa 7");
        String anterior = mesa.qrToken();
        mesa.rotacionarQrToken();
        assertThat(anterior).hasSize(32).doesNotContain("+", "/", "=");
        assertThat(mesa.qrToken()).isNotEqualTo(anterior);
    }

    @Test
    void codigo_da_estacao_e_normalizado() {
        assertThat(Estacao.cadastrar(T, null, "Sobremesa Fria", 1, 70).codigo()).isEqualTo("sobremesa-fria");
        assertThat(Estacao.normalizarCodigo("  Fritura Ácida ")).isEqualTo("fritura-acida");
    }

    @Test
    void regra_exige_exatamente_um_criterio_e_destino_existente() {
        assertThatThrownBy(() -> new RegraDeRoteamento(null, T, "item", "Bebidas", "bar", 1))
                .isInstanceOf(RegraDeNegocioException.class);
        var regra = new RegraDeRoteamento(null, T, null, "Bebidas", "Bar", 1);
        assertThat(regra.estacaoCodigo()).isEqualTo("bar");
        assertThatThrownBy(() -> RegraDeRoteamento.validarDestinos(List.of(regra),
                List.of(Estacao.cadastrar(T, "grill", "Grill", 1, 70))))
                .hasMessageContaining("bar");
    }

    @Test
    void taxa_de_servico_e_fuso_sao_validados() {
        assertThatThrownBy(() -> new Estabelecimento(T, "X", "America/Sao_Paulo", BigDecimal.valueOf(31), null, true))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> new Estabelecimento(T, "X", "Lua/Base", BigDecimal.TEN, null, true))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
