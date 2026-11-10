package br.com.smartkds.estabelecimento.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "br.com.smartkds.estabelecimento.shared", considerNestedRepositories = true)
class AplicacaoConfig {

    @Bean
    OpenAPI documentacao() {
        return new OpenAPI().info(new Info()
                .title("SmartKDS & Menu - estabelecimento-service")
                .version("1.0.0")
                .description("Configuracao do tenant: mesas e QR Codes, estacoes de producao e regras de "
                        + "roteamento item -> estacao (RF19). Rotas exigem X-Tenant-Id, exceto a resolucao do qrToken."));
    }
}
