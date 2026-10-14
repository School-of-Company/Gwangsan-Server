package team.startup.gwangsan.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 헥사고날 계층 규칙. root 패키지를 받아 실제 코드와 금지 fixture 에 같은 규칙을 적용한다.
 *
 * <pre>
 * root.domain.&lt;업무&gt;          순수 모델/정책/예외. JDK·lombok·자기 업무·domain.common 만 의존
 * root.application.&lt;업무&gt;     port/in, port/out, data(순수) + service(@Service/@Transactional 허용)
 * root.adapter.&lt;기술&gt;.&lt;업무&gt;  기술 구현. application service 직접 호출 금지(input Port 사용)
 * root.infrastructure       설정·wiring·공통 HTTP 오류 매핑
 * </pre>
 *
 * legacy 는 전환 전 클래스 목록(legacy-classes.txt)이다. legacy 클래스 자체는 검사하지 않지만,
 * 새 domain/application 이 legacy 에 의존하는 것은 금지한다(sharedKernel 제외).
 */
final class HexagonalRules {

    private static final List<String> LAYERS = List.of("domain", "application", "adapter", "infrastructure");
    private static final List<String> BUSINESS_LAYERS = List.of("domain", "application");
    private static final String COMMON = "common";

    private final String root;
    private final Set<String> legacy;
    private final Set<String> sharedKernel;

    HexagonalRules(String root, Set<String> legacy, Set<String> sharedKernel) {
        this.root = root;
        this.legacy = legacy;
        this.sharedKernel = sharedKernel;
    }

    List<ArchRule> all() {
        return List.of(
                layersAreKnown(),
                domainIsPure(),
                applicationIsPure(),
                portAndDataAreFrameworkFree(),
                businessesAreIsolated(),
                applicationSlicesAreAcyclic(),
                adaptersUseInputPorts(),
                inboundAdaptersDoNotTouchPersistence(),
                persistenceModelsLiveInPersistenceAdapter(),
                webEntryPointsLiveInWebAdapter()
        );
    }

    ArchRule layersAreKnown() {
        return classes().that(inRoot().and(notLegacy()).and(notApplicationMain()))
                .should(resideInAnyOf(LAYERS.stream().map(this::pkg).toArray(String[]::new)))
                .as("새 클래스는 domain/application/adapter/infrastructure 에 둔다 (global 신규 추가 금지)")
                .allowEmptyShould(true);
    }

    ArchRule domainIsPure() {
        return classes().that(in("domain").and(notLegacy()))
                .should().onlyDependOnClassesThat(allowedFor("java", "lombok", pkg("domain")))
                .as("domain 은 JDK·lombok·domain 순수 타입에만 의존한다")
                .allowEmptyShould(true);
    }

    ArchRule applicationIsPure() {
        return classes().that(in("application"))
                .should().onlyDependOnClassesThat(allowedFor("java", "lombok", "org.slf4j",
                        "org.springframework.stereotype", "org.springframework.transaction.annotation",
                        pkg("domain"), pkg("application")))
                .as("application 은 domain·application 과 @Service/@Transactional 외 기술 타입에 의존하지 않는다")
                .allowEmptyShould(true);
    }

    ArchRule portAndDataAreFrameworkFree() {
        return noClasses().that(inAny(pkg("application") + "..port..", pkg("application") + "..data.."))
                .should().dependOnClassesThat(inPackages("org.springframework", "org.slf4j"))
                .as("application port/data 는 Spring 타입을 쓰지 않는다")
                .allowEmptyShould(true);
    }

    ArchRule businessesAreIsolated() {
        return classes().that(in("domain").or(in("application")).and(notLegacy()))
                .should(dependOnlyOnOwnBusinessOrCommon())
                .as("domain/application 업무는 자기 업무와 common 만 참조한다 (타 업무는 adapter/integration 경유)")
                .allowEmptyShould(true);
    }

    ArchRule applicationSlicesAreAcyclic() {
        return slices().matching(root + ".application.(*)..")
                .should().beFreeOfCycles()
                .as("application 업무 간 순환 의존 금지")
                .allowEmptyShould(true);
    }

    ArchRule adaptersUseInputPorts() {
        return noClasses().that(in("adapter"))
                .should().dependOnClassesThat(inAny(pkg("application") + "..service.."))
                .as("adapter 는 application service 가 아니라 input Port 를 호출한다")
                .allowEmptyShould(true);
    }

    ArchRule inboundAdaptersDoNotTouchPersistence() {
        return noClasses().that(inAny(pkg("adapter.web") + "..", pkg("adapter.event") + "..",
                        pkg("adapter.scheduler") + "..", pkg("adapter.stream") + ".."))
                .should().dependOnClassesThat(inPackages(pkg("adapter.persistence"),
                        "jakarta.persistence", "org.springframework.data.jpa", "org.springframework.data.repository",
                        "com.querydsl"))
                .as("inbound adapter 는 persistence 모델·Repository 를 직접 쓰지 않는다")
                .allowEmptyShould(true);
    }

    ArchRule persistenceModelsLiveInPersistenceAdapter() {
        DescribedPredicate<JavaClass> persistenceModel = DescribedPredicate.describe("JPA 모델 또는 Spring Data Repository",
                c -> c.isAnnotatedWith("jakarta.persistence.Entity")
                        || c.isAnnotatedWith("jakarta.persistence.Embeddable")
                        || c.isAnnotatedWith("jakarta.persistence.MappedSuperclass")
                        || c.isAssignableTo("org.springframework.data.repository.Repository"));
        return classes().that(inRoot().and(notLegacy()).and(persistenceModel))
                .should(resideInAnyOf(pkg("adapter.persistence")))
                .as("JPA Entity/Repository 는 adapter.persistence 에만 둔다")
                .allowEmptyShould(true);
    }

    ArchRule webEntryPointsLiveInWebAdapter() {
        DescribedPredicate<JavaClass> webEntry = DescribedPredicate.describe("Controller/ControllerAdvice",
                c -> c.isAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                        || c.isAnnotatedWith("org.springframework.stereotype.Controller")
                        || c.isAnnotatedWith("org.springframework.web.bind.annotation.RestControllerAdvice")
                        || c.isAnnotatedWith("org.springframework.web.bind.annotation.ControllerAdvice"));
        return classes().that(inRoot().and(notLegacy()).and(webEntry))
                .should(resideInAnyOf(pkg("adapter.web"), pkg("infrastructure.web")))
                .as("Controller 는 adapter.web, 공통 오류 매핑은 infrastructure.web 에 둔다")
                .allowEmptyShould(true);
    }

    // --- predicates / conditions ---

    boolean isLegacy(JavaClass c) {
        String name = c.getName();
        int nested = name.indexOf('$');
        String top = nested < 0 ? name : name.substring(0, nested);
        if (legacy.contains(top)) {
            return true;
        }
        // QueryDSL 이 legacy Entity 옆에 생성하는 Q 타입도 legacy 로 본다.
        String simple = top.substring(top.lastIndexOf('.') + 1);
        return simple.length() > 1 && simple.charAt(0) == 'Q'
                && legacy.contains(top.substring(0, top.lastIndexOf('.') + 1) + simple.substring(1));
    }

    private DescribedPredicate<JavaClass> allowedFor(String... packages) {
        return DescribedPredicate.describe("허용 패키지 " + Arrays.toString(packages), target -> {
            JavaClass base = target.getBaseComponentType();
            if (base.isPrimitive() || sharedKernel.contains(base.getName())) {
                return true;
            }
            if (isLegacy(base)) {
                return false;
            }
            return inPackage(base.getPackageName(), packages);
        });
    }

    private ArchCondition<JavaClass> dependOnlyOnOwnBusinessOrCommon() {
        return new ArchCondition<>("자기 업무 또는 common 만 참조") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String own = business(item);
                for (Dependency d : item.getDirectDependenciesFromSelf()) {
                    String target = business(d.getTargetClass().getBaseComponentType());
                    if (target != null && !target.equals(own) && !target.equals(COMMON)) {
                        events.add(SimpleConditionEvent.violated(d, d.getDescription()));
                    }
                }
            }
        };
    }

    private String business(JavaClass c) {
        String p = c.getPackageName();
        for (String layer : BUSINESS_LAYERS) {
            String prefix = pkg(layer) + ".";
            if (p.startsWith(prefix)) {
                return p.substring(prefix.length()).split("\\.")[0];
            }
        }
        return null;
    }

    private ArchCondition<JavaClass> resideInAnyOf(String... packages) {
        return new ArchCondition<>("reside in " + Arrays.toString(packages)) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!inPackage(item.getPackageName(), packages)) {
                    events.add(SimpleConditionEvent.violated(item, item.getName() + " 위치 위반"));
                }
            }
        };
    }

    private DescribedPredicate<JavaClass> inRoot() {
        return inPackages(root);
    }

    private DescribedPredicate<JavaClass> in(String layer) {
        return inPackages(pkg(layer));
    }

    private DescribedPredicate<JavaClass> inAny(String... archUnitPatterns) {
        return DescribedPredicate.describe("in " + Arrays.toString(archUnitPatterns),
                c -> com.tngtech.archunit.core.domain.PackageMatchers.of(archUnitPatterns).test(c.getPackageName()));
    }

    private DescribedPredicate<JavaClass> notLegacy() {
        return DescribedPredicate.describe("legacy 가 아닌", c -> !isLegacy(c));
    }

    private DescribedPredicate<JavaClass> notApplicationMain() {
        return DescribedPredicate.describe("Application main 이 아닌",
                c -> !(c.getPackageName().equals(root) && c.isAnnotatedWith("org.springframework.boot.autoconfigure.SpringBootApplication")));
    }

    private static DescribedPredicate<JavaClass> inPackages(String... packages) {
        return DescribedPredicate.describe("in " + Arrays.toString(packages),
                c -> inPackage(c.getBaseComponentType().getPackageName(), packages));
    }

    private static boolean inPackage(String pkg, String... packages) {
        for (String p : packages) {
            if (pkg.equals(p) || pkg.startsWith(p + ".")) {
                return true;
            }
        }
        return false;
    }

    private String pkg(String layer) {
        return root + "." + layer;
    }
}
