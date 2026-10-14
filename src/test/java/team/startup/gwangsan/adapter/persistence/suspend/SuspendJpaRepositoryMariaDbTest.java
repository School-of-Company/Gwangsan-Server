package team.startup.gwangsan.adapter.persistence.suspend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.infrastructure.config.QueryDslConfig;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** 해제 대상 JPQL(DISTINCT·NOT EXISTS·회원 상태 조건)을 운영과 같은 MariaDB 에서 실행한다. */
@DataJpaTest
@Import(QueryDslConfig.class)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("SuspendJpaRepository 해제 대상 조회 (MariaDB)")
class SuspendJpaRepositoryMariaDbTest {

    @Container
    static final MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.4");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", mariadb::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired private TestEntityManager em;
    @Autowired private SuspendJpaRepository suspendJpaRepository;

    private final LocalDateTime until = LocalDateTime.of(2026, 10, 20, 10, 0);

    @Test
    @DisplayName("it_만료 정지만 있는 SUSPENDED 회원만, 재정지·다른 상태는 제외하고, 회원당 한 번 반환한다")
    void it_selects_only_releasable_members() {
        MemberJpaEntity expired = member("만료", MemberStatus.SUSPENDED);
        suspend(expired, until.minusDays(10));
        suspend(expired, until.minusDays(1));
        MemberJpaEntity resuspended = member("재정지", MemberStatus.SUSPENDED);
        suspend(resuspended, until.minusDays(10));
        suspend(resuspended, until.plusDays(3));
        MemberJpaEntity pending = member("대기", MemberStatus.PENDING);
        suspend(pending, until.minusDays(1));
        MemberJpaEntity withdrawn = member("탈퇴", MemberStatus.WITHDRAWN);
        suspend(withdrawn, until.minusDays(1));
        em.flush();
        em.clear();

        assertThat(suspendJpaRepository.findReleasableMemberIds(until)).containsExactly(expired.getId());
    }

    @Test
    @DisplayName("it_정지 종료 시각과 같은 시각에는 해제하지 않고 1초 뒤부터 해제한다")
    void it_keeps_boundary() {
        MemberJpaEntity member = member("경계", MemberStatus.SUSPENDED);
        suspend(member, until.minusDays(10));
        suspend(member, until);
        em.flush();
        em.clear();

        assertThat(suspendJpaRepository.findReleasableMemberIds(until.minusSeconds(1))).isEmpty();
        assertThat(suspendJpaRepository.findReleasableMemberIds(until)).isEmpty();
        assertThat(suspendJpaRepository.findReleasableMemberIds(until.plusSeconds(1))).containsExactly(member.getId());
    }

    private MemberJpaEntity member(String suffix, MemberStatus status) {
        return em.persist(MemberJpaEntity.builder()
                .name("이름-" + suffix)
                .nickname("회원-" + suffix)
                .password("pw")
                .phoneNumber("010-7" + String.format("%03d", Math.floorMod(suffix.hashCode(), 1000)) + "-0000")
                .role(MemberRole.ROLE_USER)
                .status(status)
                .build());
    }

    private void suspend(MemberJpaEntity member, LocalDateTime suspendedUntil) {
        em.persist(SuspendJpaEntity.builder().member(member).suspendedAt(suspendedUntil.minusDays(1)).suspendedDays(1)
                .suspendedUntil(suspendedUntil).build());
    }
}
