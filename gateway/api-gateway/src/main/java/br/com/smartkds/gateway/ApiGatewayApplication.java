package br.com.smartkds.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Unica porta de entrada do SmartKDS & Menu (ADR-02). Concentra apenas preocupacoes de borda:
 * validacao de JWT, CORS, limitacao de taxa, correlacao e roteamento por prefixo para os BFFs.
 * Nao contem regra de negocio nem composicao de resposta.
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
