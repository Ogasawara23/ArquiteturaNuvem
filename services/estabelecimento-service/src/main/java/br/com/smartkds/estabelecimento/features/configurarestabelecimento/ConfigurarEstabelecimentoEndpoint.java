package br.com.smartkds.estabelecimento.features.configurarestabelecimento;

import br.com.smartkds.estabelecimento.shared.dominio.Estabelecimento;
import br.com.smartkds.estabelecimento.shared.tenancy.ContextoDoTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Estabelecimento")
class ConfigurarEstabelecimentoEndpoint {

    private final ConfigurarEstabelecimentoHandler handler;

    ConfigurarEstabelecimentoEndpoint(ConfigurarEstabelecimentoHandler handler) {
        this.handler = handler;
    }

    @PutMapping("/interno/estabelecimento")
    @Operation(summary = "Provisiona ou atualiza o estabelecimento do tenant (nome, fuso, taxa de servico)")
    public ConfigurarEstabelecimentoResponse configurar(@Valid @RequestBody ConfigurarEstabelecimentoRequest r) {
        Estabelecimento e = handler.executar(new Estabelecimento(ContextoDoTenant.atual(), r.nomeFantasia(),
                r.fusoHorario(), r.percentualTaxaServico(), r.estacaoPadrao(), true));
        return new ConfigurarEstabelecimentoResponse(e.tenantId().valor(), e.nomeFantasia(), e.fusoHorario(),
                e.percentualTaxaServico(), e.estacaoPadrao());
    }
}
