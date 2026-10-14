package team.startup.gwangsan.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PR12 최종 gate: 전면 전환이 끝났을 때만 통과한다 (계획 작업 15·F1, IS-1/IS-6).
 * PR02~PR11 이 끝나기 전에는 반드시 실패하므로 HEXAGONAL_FINAL_GATE=true 일 때만 실행한다.
 * ponytail: PR12 최종 단계에서 @EnabledIfEnvironmentVariable 를 지워 항상 실행되게 한다 (skip 0 조건).
 */
@EnabledIfEnvironmentVariable(named = "HEXAGONAL_FINAL_GATE", matches = "true")
@DisplayName("헥사고날 전면 전환 최종 gate")
class HexagonalFinalGateTest {

    private static final String ROOT = "team.startup.gwangsan";
    private static final Pattern LEGACY_PACKAGE =
            Pattern.compile(Pattern.quote(ROOT) + "\\.domain\\.[a-z]+\\.(service|repository|entity|presentation)(\\..*)?");
    private static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Nested
    @DisplayName("전환 완료 상태는")
    class Describe_completed {

        @Test
        @DisplayName("it_검사 대상 클래스를 실제로 import 한다")
        void it_imports_real_classes() {
            assertThat(PRODUCTION.size()).isGreaterThan(500);
        }

        @Test
        @DisplayName("it_legacy 목록이 비어 있다 (남은 항목을 PR 별로 보고)")
        void it_has_empty_legacy_list() {
            Map<String, Long> remainingByPr = legacyLines().stream()
                    .collect(Collectors.groupingBy(line -> line.split("\\s+")[1], TreeMap::new, Collectors.counting()));

            assertThat(remainingByPr).as("legacy-classes.txt 남은 항목 수(PR 별)").isEmpty();
        }

        @Test
        @DisplayName("it_global 패키지 클래스가 없다")
        void it_has_no_global_package() {
            assertThat(names(c -> c.getPackageName().startsWith(ROOT + ".global"))).isEmpty();
        }

        @Test
        @DisplayName("it_기존 domain/<업무>/{service,repository,entity,presentation} 패키지가 없다")
        void it_has_no_legacy_business_package() {
            assertThat(names(c -> LEGACY_PACKAGE.matcher(c.getPackageName()).matches())).isEmpty();
        }

        @Test
        @DisplayName("it_공통 예외가 domain/common 의 순수 타입이다")
        void it_moved_shared_exceptions() {
            assertThat(names(c -> c.getSimpleName().equals("ErrorCode") || c.getSimpleName().equals("GlobalException")))
                    .isNotEmpty()
                    .allMatch(name -> name.startsWith(ROOT + ".domain.common."));
        }
    }

    private static List<String> names(java.util.function.Predicate<JavaClass> predicate) {
        return PRODUCTION.stream().filter(predicate).map(JavaClass::getName).sorted().toList();
    }

    private static List<String> legacyLines() {
        try (InputStream in = HexagonalFinalGateTest.class.getClassLoader()
                .getResourceAsStream("architecture/legacy-classes.txt")) {
            if (in == null) {
                return List.of();
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
