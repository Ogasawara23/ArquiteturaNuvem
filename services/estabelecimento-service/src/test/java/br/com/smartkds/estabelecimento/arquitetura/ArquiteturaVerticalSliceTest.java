package br.com.smartkds.estabelecimento.arquitetura;

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

/** Secao 8.8.1: o estilo arquitetural verificado a cada build. */
@AnalyzeClasses(packages = "br.com.smartkds.estabelecimento", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaVerticalSliceTest {

    private static final String RAIZ = "br.com.smartkds.estabelecimento";
    private static final String FEATURES = RAIZ + ".features..";
    private static final String SHARED = RAIZ + ".shared..";
    private static final String CONFIG = RAIZ + ".config..";
    private static final String DOMINIO = RAIZ + ".shared.dominio..";

    // 1. A REGRA CENTRAL DO VERTICAL SLICE: fatias nao se conhecem
    @ArchTest
    static final ArchRule fatias_nao_dependem_umas_das_outras =
            slices().matching(RAIZ + ".features.(*)..")
                    .should().notDependOnEachOther()
                    .because("cada fatia e uma unidade autonoma de funcionalidade; "
                            + "reuso entre fatias deve passar pelo dominio ou pelo shared");

    @ArchTest
    static final ArchRule fatias_sao_livres_de_ciclos =
            slices().matching(RAIZ + ".features.(*)..").should().beFreeOfCycles();

    // 2. PROIBICAO EXPLICITA DE PACOTES TECNICOS GLOBAIS
    @ArchTest
    static final ArchRule nao_existem_pacotes_de_camada_tecnica =
            noClasses().should().resideInAnyPackage(
                            RAIZ + ".controllers..", RAIZ + ".controller..",
                            RAIZ + ".services..", RAIZ + ".service..",
                            RAIZ + ".repositories..", RAIZ + ".repository..",
                            RAIZ + ".dto..", RAIZ + ".model..")
                    .because("a organizacao e por funcionalidade (Vertical Slice), nunca por camada tecnica");

    // 3. DIRECAO DAS DEPENDENCIAS ENTRE FEATURES, SHARED E CONFIG
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
                    .because("o agregado nao pode conhecer Spring, JPA, Jackson nem AMQP");

    // 4. ANATOMIA OBRIGATORIA DE UMA FATIA
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
                    .because("visibilidade de pacote impede que outra fatia importe o handler, "
                            + "reforcando a regra 1 no proprio compilador");

    // 5. ISOLAMENTO DE PERSISTENCIA E MULTI-TENANCY
    @ArchTest
    static final ArchRule apenas_shared_persistencia_usa_jpa =
            noClasses().that().resideInAPackage(FEATURES)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..", "org.hibernate..", "org.springframework.data..", "org.springframework.jdbc..")
                    .because("o acesso ao banco passa pelos repositorios do shared");

    @ArchTest
    static final ArchRule repositorios_recebem_o_tenant_na_assinatura =
            methods().that().areDeclaredInClassesThat().resideInAPackage(RAIZ + ".shared.persistencia..")
                    .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Repository")
                    .and().arePublic()
                    .should(receberTenantIdComoPrimeiroParametro())
                    .because("o isolamento multi-tenant nao pode depender de estado implicito "
                            + "nem de disciplina do desenvolvedor");

    @ArchTest
    static final ArchRule nenhuma_classe_acessa_banco_de_outro_servico =
            noClasses().should().dependOnClassesThat()
                    .haveNameMatching(".*(pedido|catalogo|producao)\\..*Entity")
                    .because("Database per Service: cada servico so conhece o proprio modelo");

    // 6. PUBLICACAO DE EVENTOS SEMPRE PELA OUTBOX
    // CONDICOES CUSTOMIZADAS
    private static ArchCondition<JavaClass> pertencerAFatiaComPontoDeEntradaUnico() {
        return new ArchCondition<>("pertencer a uma fatia com um unico Handler ou Query") {
            @Override
            public void check(JavaClass classe, ConditionEvents eventos) {
                long pontosDeEntrada = classe.getPackage().getClasses().stream()
                        .filter(c -> c.getSimpleName().endsWith("Handler") || c.getSimpleName().endsWith("Query"))
                        .count();
                if (pontosDeEntrada != 1) {
                    eventos.add(SimpleConditionEvent.violated(classe, "a fatia " + classe.getPackageName()
                            + " possui " + pontosDeEntrada + " pontos de orquestracao; o esperado e exatamente 1"));
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
