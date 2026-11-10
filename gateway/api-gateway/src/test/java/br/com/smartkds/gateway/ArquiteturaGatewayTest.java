package br.com.smartkds.gateway;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** O gateway e borda pura (ADR-02): sem controllers, sem acesso a dados e sem chamar microsservicos. */
@AnalyzeClasses(packages = "br.com.smartkds.gateway", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaGatewayTest {

    @ArchTest
    static final ArchRule gateway_nao_tem_endpoints_de_negocio =
            noClasses().should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .because("o gateway nao contem regra de negocio nem composicao de resposta");

    @ArchTest
    static final ArchRule gateway_nao_acessa_dados =
            noClasses().should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.data..", "jakarta.persistence..", "org.springframework.jdbc..")
                    .because("o gateway nao possui banco");

    @ArchTest
    static final ArchRule gateway_nao_compoe_respostas =
            noClasses().should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web.reactive.function.client..")
                    .because("composicao e responsabilidade dos BFFs (ADR-03)");
}
