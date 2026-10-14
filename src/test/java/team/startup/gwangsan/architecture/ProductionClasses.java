package team.startup.gwangsan.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/** production 클래스 ArchUnit import 를 테스트 JVM 에서 한 번만 한다(아키텍처·gate·호환 테스트 공유, 힙 절약). */
public final class ProductionClasses {

    public static final String ROOT = "team.startup.gwangsan";
    public static final JavaClasses ALL = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    private ProductionClasses() {
    }
}
