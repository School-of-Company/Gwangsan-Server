package team.startup.gwangsan.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.image.service.CleanupOrphanImagesService;
import team.startup.gwangsan.domain.image.policy.OrphanImagePolicy;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("image core 경계 (PR05)")
class OrphanImageBoundaryTest {

    private static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("team.startup.gwangsan");

    @Test
    @DisplayName("it_image domain model/policy·application 은 S3·AI·QueryDSL·JPA·타 업무 legacy 에 의존하지 않는다")
    void it_keeps_image_core_free_of_external_and_other_business_types() {
        assertThat(PRODUCTION.contain(CleanupOrphanImagesService.class)).isTrue();
        assertThat(PRODUCTION.contain(OrphanImagePolicy.class)).isTrue();

        noClasses().that().resideInAnyPackage(
                        "team.startup.gwangsan.domain.image.model..",
                        "team.startup.gwangsan.domain.image.policy..",
                        "team.startup.gwangsan.application.image..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "software.amazon..",
                        "org.springframework.web..",
                        "com.querydsl..",
                        "jakarta.persistence..",
                        "team.startup.gwangsan.global.thirdparty..",
                        "team.startup.gwangsan.adapter..",
                        "team.startup.gwangsan.domain.chat..",
                        "team.startup.gwangsan.domain.notice..",
                        "team.startup.gwangsan.domain.post..",
                        "team.startup.gwangsan.domain.report..",
                        "team.startup.gwangsan.domain.trade..")
                .check(PRODUCTION);
    }
}
