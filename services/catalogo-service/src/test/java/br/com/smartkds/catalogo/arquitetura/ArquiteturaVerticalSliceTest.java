package br.com.smartkds.catalogo.arquitetura;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Testes unitarios de arquitetura (Secao 8.8): transformam o Vertical Slice e a
 * separacao Dominio / Aplicacao / Infraestrutura / API em falha de build.
 */
@AnalyzeClasses(packages = "br.com.smartkds.catalogo", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaVerticalSliceTest {

    private static final String RAIZ = "br.com.smartkds.catalogo";
    private static final String FEATURES = RAIZ + ".features..";
    private static final String SHARED = RAIZ + ".shared..";
    private static final String CONFIG = RAIZ + ".config..";
    private static final String DOMINIO = RAIZ + ".shared.dominio..";
    private static final String PERSISTENCIA = RAIZ + ".shared.persistencia..";

    // 1. A regra central do Vertical Slice: fatias nao se conhecem
    @ArchTest
    static final ArchRule fatias_nao_dependem_umas_das_outras =
            slices().matching(RAIZ + ".features.(*)..")
                    .should().notDependOnEachOther()
                    .because("cada fatia e uma unidade autonoma; reuso passa pelo dominio ou pelo shared");

    @ArchTest
    static final ArchRule fatias_sao_livres_de_ciclos =
            slices().matching(RAIZ + ".features.(*)..").should().beFreeOfCycles();

    // 2. Proibicao de pacotes de camada tecnica globais
    @ArchTest
    static final ArchRule nao_existem_pacotes_de_camada_tecnica =
            noClasses().should().resideInAnyPackage(
                            RAIZ + ".controller..", RAIZ + ".controllers..",
                            RAIZ + ".service..", RAIZ + ".services..",
                            RAIZ + ".repository..", RAIZ + ".repositories..",
                            RAIZ + ".dto..", RAIZ + ".model..")
                    .because("a organizacao e por funcionalidade, nunca por camada tecnica");

    // 3. Direcao das dependencias (Clean Architecture dentro do Vertical Slice)
    @ArchTest
    static final ArchRule shared_nao_depende_de_features =
            noClasses().that().resideInAPackage(SHARED)
                    .should().dependOnClassesThat().resideInAPackage(FEATURES)
                    .because("o nucleo compartilhado nao pode conhecer funcionalidades");

    @ArchTest
    static final ArchRule features_nao_dependem_de_config =
            noClasses().that().resideInAPackage(FEATURES)
                    .should().dependOnClassesThat().resideInAPackage(CONFIG)
                    .because("a composicao conhece as fatias, nunca o contrario");

    @ArchTest
    static final ArchRule dominio_e_java_puro =
            classes().that().resideInAPackage(DOMINIO)
                    .should().onlyDependOnClassesThat().resideInAnyPackage(DOMINIO, "java..")
                    .because("o agregado nao pode conhecer Spring, MongoDB, Jackson nem HTTP");

    @ArchTest
    static final ArchRule dominio_nao_depende_de_infraestrutura =
            noClasses().that().resideInAPackage(DOMINIO)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            RAIZ + ".shared.persistencia..", RAIZ + ".shared.web..", RAIZ + ".shared.tenancy..");

    // 4. Anatomia obrigatoria de uma fatia
    @ArchTest
    static final ArchRule toda_fatia_possui_exatamente_um_handler_ou_query =
            classes().that().resideInAPackage(FEATURES).and().areTopLevelClasses()
                    .should(pertencerAFatiaComPontoDeEntradaUnico())
                    .because("uma fatia expressa uma funcionalidade, com um unico ponto de orquestracao");

    @ArchTest
    static final ArchRule handlers_terminam_com_Handler_ou_Query =
            classes().that().resideInAPackage(FEATURES).and().areTopLevelClasses()
                    .and().haveSimpleNameNotEndingWith("Endpoint")
                    .and().haveSimpleNameNotEndingWith("Listener")
                    .and().haveSimpleNameNotEndingWith("Request")
                    .and().haveSimpleNameNotEndingWith("Response")
                    .and().haveSimpleNameNotEndingWith("Command")
                    .and().haveSimpleNameNotEndingWith("Dto")
                    .and().haveSimpleNameNotEndingWith("Message")
                    .and().haveSimpleNameNotEndingWith("Validator")
                    .and().haveSimpleNameNotEndingWith("Factory")
                    .and().haveSimpleNameNotEndingWith("Projection")
                    .should().haveSimpleNameEndingWith("Handler")
                    .orShould().haveSimpleNameEndingWith("Query")
                    .because("a nomenclatura torna a anatomia da fatia previsivel");

    @ArchTest
    static final ArchRule endpoints_vivem_dentro_de_uma_fatia =
            classes().that().areAnnotatedWith(RestController.class)
                    .should().resideInAPackage(FEATURES)
                    .andShould().haveSimpleNameEndingWith("Endpoint")
                    .because("nao existe controller fora de uma funcionalidade");

    @ArchTest
    static final ArchRule classes_da_fatia_nao_sao_publicas =
            classes().that().resideInAPackage(FEATURES).and().areTopLevelClasses()
                    .should().notBePublic()
                    .because("visibilidade de pacote impede que outra fatia importe a classe");

    // 5. Isolamento de persistencia e multi-tenancy
    @ArchTest
    static final ArchRule apenas_shared_persistencia_conhece_o_banco =
            noClasses().that().resideOutsideOfPackages(PERSISTENCIA, CONFIG)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.data.mongodb..", "com.mongodb..", "org.bson..")
                    .because("o acesso ao MongoDB passa pelos repositorios do shared");

    @ArchTest
    static final ArchRule repositorios_recebem_o_tenant_na_assinatura =
            methods().that().areDeclaredInClassesThat().resideInAPackage(PERSISTENCIA)
                    .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Repository")
                    .and().arePublic()
                    .should(receberTenantIdComoPrimeiroParametro())
                    .because("o isolamento multi-tenant nao pode depender de estado implicito");

    private static ArchCondition<JavaClass> pertencerAFatiaComPontoDeEntradaUnico() {
        return new ArchCondition<>("pertencer a uma fatia com um unico Handler ou Query") {
            @Override
            public void check(JavaClass classe, ConditionEvents eventos) {
                long pontos = classe.getPackage().getClasses().stream()
                        .filter(c -> c.getSimpleName().endsWith("Handler") || c.getSimpleName().endsWith("Query"))
                        .count();
                if (pontos != 1) {
                    eventos.add(SimpleConditionEvent.violated(classe, "a fatia " + classe.getPackageName()
                            + " possui " + pontos + " pontos de orquestracao; o esperado e exatamente 1"));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> receberTenantIdComoPrimeiroParametro() {
        return new ArchCondition<>("receber TenantId como primeiro parametro") {
            @Override
            public void check(JavaMethod metodo, ConditionEvents eventos) {
                var parametros = metodo.getRawParameterTypes();
                boolean conforme = !parametros.isEmpty() && parametros.get(0).getSimpleName().equals("TenantId");
                boolean isento = metodo.getName().startsWith("salvar");
                if (!conforme && !isento) {
                    eventos.add(SimpleConditionEvent.violated(metodo,
                            metodo.getFullName() + " nao recebe TenantId como primeiro parametro"));
                }
            }
        };
    }
}
