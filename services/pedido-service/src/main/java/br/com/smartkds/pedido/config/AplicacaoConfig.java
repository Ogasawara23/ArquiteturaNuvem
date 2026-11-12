package br.com.smartkds.pedido.config;

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
@EnableJpaRepositories(basePackages = "br.com.smartkds.pedido.shared", considerNestedRepositories = true)
class AplicacaoConfig {

    @Bean
    Clock relogio() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    @Bean
    OpenAPI documentacao() {
        return new OpenAPI().info(new Info()
                .title("SmartKDS & Menu - pedido-service")
                .version("1.0.0")
                .description("Microsservico 2 (Azure SQL Database): comanda eletronica, confirmacao de pedido com "
                        + "Transactional Outbox, cancelamento, fechamento e auditoria. "
                        + "Rotas internas exigem o cabecalho X-Tenant-Id."));
    }
}
