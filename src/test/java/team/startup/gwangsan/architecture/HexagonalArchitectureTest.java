package team.startup.gwangsan.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import team.startup.gwangsan.GwangsanApplication;
import team.startup.gwangsan.adapter.security.auth.ActorContextArgumentResolver;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;
import team.startup.gwangsan.infrastructure.security.SecurityConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("헥사고날 아키텍처 경계")
class HexagonalArchitectureTest {

    private static final String ROOT = "team.startup.gwangsan";
    private static final String LEGACY_LIST = "architecture/legacy-classes.txt";
    // 순수 JDK 타입인 기존 공통 예외. 업무 PR 들이 같은 예외 규칙을 쓰도록 허용하고 PR12 에서 domain/common 으로 옮긴다.
    private static final Set<String> SHARED_KERNEL = Set.of(GlobalException.class.getName(), ErrorCode.class.getName());

    private static final Map<String, String> LEGACY = loadLegacy();
    private static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);
    private static final HexagonalRules PRODUCTION_RULES = new HexagonalRules(ROOT, LEGACY.keySet(), SHARED_KERNEL);

    @Nested
    @DisplayName("실제 production 코드는")
    class Describe_production {

        @Test
        @DisplayName("it_검사 대상 클래스를 실제로 import 한다 (빈 대상 통과 방지)")
        void it_imports_real_classes() {
            assertThat(PRODUCTION.size()).isGreaterThan(500);
            assertThat(PRODUCTION.contain(GwangsanApplication.class)).isTrue();
            // 새 계층의 클래스가 규칙 대상에 실제로 포함되는지 확인한다.
            for (Class<?> migrated : new Class<?>[]{ActorContext.class, ActorContextArgumentResolver.class, SecurityConfig.class}) {
                assertThat(PRODUCTION.contain(migrated)).as(migrated.getName()).isTrue();
                assertThat(PRODUCTION_RULES.isLegacy(PRODUCTION.get(migrated))).as(migrated.getName()).isFalse();
            }
        }

        @TestFactory
        @DisplayName("it_모든 계층 규칙을 만족한다")
        Stream<DynamicTest> it_satisfies_rules() {
            return PRODUCTION_RULES.all().stream()
                    .map(rule -> DynamicTest.dynamicTest(rule.getDescription(), () -> rule.check(PRODUCTION)));
        }

        @Test
        @DisplayName("it_공유 예외 타입은 JDK·lombok 에만 의존한다")
        void it_keeps_shared_kernel_pure() {
            classes().that().haveFullyQualifiedName(GlobalException.class.getName())
                    .or().haveFullyQualifiedName(ErrorCode.class.getName())
                    .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "lombok..", "team.startup.gwangsan.global.exception..")
                    .check(PRODUCTION);
        }
    }

    @Nested
    @DisplayName("legacy 목록은")
    class Describe_legacy_list {

        @Test
        @DisplayName("it_모든 항목이 제거 담당 PR 을 가진다")
        void it_has_removal_pr() {
            assertThat(LEGACY).isNotEmpty();
            assertThat(LEGACY.values()).allMatch(pr -> pr.matches("PR(0[2-9]|1[0-2])"));
        }

        @Test
        @DisplayName("it_현재 존재하는 domain/global 클래스만 가진다 (이동·삭제 후 남은 항목 금지)")
        void it_has_no_stale_entry() {
            Set<String> existing = PRODUCTION.stream().map(JavaClass::getName).collect(java.util.stream.Collectors.toSet());
            assertThat(LEGACY.keySet())
                    .as("이동/삭제한 클래스는 legacy-classes.txt 에서 지운다")
                    .allMatch(existing::contains)
                    .allMatch(name -> name.startsWith(ROOT + ".domain.") || name.startsWith(ROOT + ".global."));
        }
    }

    @Nested
    @DisplayName("금지 의존 fixture 에 대해")
    class Describe_fixture {

        private static final String LEGAL = "team.startup.archfixture.legal";
        private static final String ILLEGAL = "team.startup.archfixture.illegal";
        private static final JavaClasses LEGAL_CLASSES = new ClassFileImporter().importPackages(LEGAL);
        private static final JavaClasses ILLEGAL_CLASSES = new ClassFileImporter().importPackages(ILLEGAL);
        private static final HexagonalRules LEGAL_RULES = new HexagonalRules(LEGAL, Set.of(), Set.of());
        private static final HexagonalRules ILLEGAL_RULES = new HexagonalRules(ILLEGAL,
                Set.of(ILLEGAL + ".domain.order.entity.LegacyOrderEntity"), Set.of());

        @TestFactory
        @DisplayName("it_합법 port/adapter 구조는 모든 규칙을 통과한다")
        Stream<DynamicTest> it_passes_legal_structure() {
            assertThat(LEGAL_CLASSES.size()).isGreaterThanOrEqualTo(9);
            return LEGAL_RULES.all().stream()
                    .map(rule -> DynamicTest.dynamicTest(rule.getDescription(), () -> rule.check(LEGAL_CLASSES)));
        }

        static Stream<Arguments> violations() {
            Function<HexagonalRules, ArchRule> domain = HexagonalRules::domainIsPure;
            Function<HexagonalRules, ArchRule> application = HexagonalRules::applicationIsPure;
            Function<HexagonalRules, ArchRule> isolation = HexagonalRules::businessesAreIsolated;
            return Stream.of(
                    Arguments.of("domain annotation(@Entity)", domain, "AnnotatedDomainOrder"),
                    Arguments.of("domain 상속(Spring Data Repository)", domain, "InheritingDomainRepository"),
                    Arguments.of("domain generic(List<HttpServletRequest>)", domain, "GenericDomainOrder"),
                    Arguments.of("domain → legacy Entity", domain, "LegacyDependentOrder"),
                    Arguments.of("application 메서드 호출(SecurityContextHolder)", application, "SecurityReadingService"),
                    Arguments.of("application → persistence Entity", application, "PersistenceUsingService"),
                    Arguments.of("input Port 의 HTTP 타입", application, "HttpLeakingUseCase"),
                    Arguments.of("data 의 Spring annotation", (Function<HexagonalRules, ArchRule>) HexagonalRules::portAndDataAreFrameworkFree, "SpringCommand"),
                    Arguments.of("domain 타 업무 참조", isolation, "CrossBusinessOrder"),
                    Arguments.of("application 타 업무 service 참조", isolation, "CrossBusinessService"),
                    Arguments.of("application 업무 순환", (Function<HexagonalRules, ArchRule>) HexagonalRules::applicationSlicesAreAcyclic, "AUseCase"),
                    Arguments.of("adapter → application service", (Function<HexagonalRules, ArchRule>) HexagonalRules::adaptersUseInputPorts, "ServiceCallingController"),
                    Arguments.of("inbound adapter → Repository", (Function<HexagonalRules, ArchRule>) HexagonalRules::inboundAdaptersDoNotTouchPersistence, "RepositoryCallingController"),
                    Arguments.of("persistence 밖 JPA Entity", (Function<HexagonalRules, ArchRule>) HexagonalRules::persistenceModelsLiveInPersistenceAdapter, "EntityInWeb"),
                    Arguments.of("domain 의 JPA Entity 위치", (Function<HexagonalRules, ArchRule>) HexagonalRules::persistenceModelsLiveInPersistenceAdapter, "AnnotatedDomainOrder"),
                    Arguments.of("application 의 Controller", (Function<HexagonalRules, ArchRule>) HexagonalRules::webEntryPointsLiveInWebAdapter, "ControllerInApplication"),
                    Arguments.of("global 신규 클래스", (Function<HexagonalRules, ArchRule>) HexagonalRules::layersAreKnown, "NewGlobalUtil")
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("violations")
        @DisplayName("it_금지 의존을 위반으로 보고한다")
        void it_reports_violation(String name, Function<HexagonalRules, ArchRule> rule, String offender) {
            EvaluationResult result = rule.apply(ILLEGAL_RULES).evaluate(ILLEGAL_CLASSES);

            assertThat(result.hasViolation()).as(name).isTrue();
            assertThat(result.getFailureReport().getDetails()).as(name)
                    .anyMatch(line -> line.contains(offender));
        }

        @Test
        @DisplayName("it_legacy 목록의 클래스 자체는 위반으로 보고하지 않는다")
        void it_exempts_legacy_class_itself() {
            EvaluationResult result = ILLEGAL_RULES.persistenceModelsLiveInPersistenceAdapter().evaluate(ILLEGAL_CLASSES);

            assertThat(result.getFailureReport().getDetails()).noneMatch(line -> line.contains("LegacyOrderEntity "));
        }
    }

    private static Map<String, String> loadLegacy() {
        Map<String, String> legacy = new LinkedHashMap<>();
        try (InputStream in = HexagonalArchitectureTest.class.getClassLoader().getResourceAsStream(LEGACY_LIST)) {
            if (in == null) {
                throw new IllegalStateException(LEGACY_LIST + " 없음");
            }
            new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(line -> {
                        String[] parts = line.split("\\s+");
                        if (parts.length != 2 || legacy.put(parts[0], parts[1]) != null) {
                            throw new IllegalStateException("잘못되었거나 중복된 legacy 항목: " + line);
                        }
                    });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return legacy;
    }
}
