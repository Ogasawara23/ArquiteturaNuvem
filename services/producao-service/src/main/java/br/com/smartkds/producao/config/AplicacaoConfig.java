package br.com.smartkds.producao.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
@EnableJpaRepositories(basePackages = "br.com.smartkds.producao.shared", considerNestedRepositories = true)
class AplicacaoConfig {

    @Bean
    Clock relogio() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    @Bean
    OpenAPI documentacao() {
        return new OpenAPI().info(new Info()
                .title("SmartKDS & Menu - producao-service")
                .version("1.0.0")
                .description("Kitchen Display System: tickets por estacao gerados a partir de PedidoConfirmado, "
                        + "transicoes, bump, SLA e indicadores. Rotas internas exigem X-Tenant-Id."));
    }
}
