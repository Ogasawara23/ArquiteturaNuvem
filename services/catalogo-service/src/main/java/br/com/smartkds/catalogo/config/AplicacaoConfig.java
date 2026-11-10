package br.com.smartkds.catalogo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableMongoRepositories(basePackages = "br.com.smartkds.catalogo.shared.persistencia",
        considerNestedRepositories = true)
class AplicacaoConfig {

    @Bean
    Clock relogio() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    @Bean
    OpenAPI documentacao() {
        return new OpenAPI().info(new Info()
                .title("SmartKDS & Menu - catalogo-service")
                .version("1.0.0")
                .description("Microsservico 1 (MongoDB): itens do cardapio, modificadores e versoes imutaveis. "
                        + "Todas as rotas exigem o cabecalho X-Tenant-Id (injetado pelo API Gateway ou pelo BFF)."));
    }
}
