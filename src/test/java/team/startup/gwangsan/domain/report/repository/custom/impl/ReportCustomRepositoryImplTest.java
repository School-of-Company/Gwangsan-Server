package team.startup.gwangsan.domain.report.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.report.entity.Report;
import team.startup.gwangsan.domain.report.entity.constant.ReportType;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("ReportCustomRepositoryImpl H2 슬라이스 통합 테스트")
class ReportCustomRepositoryImplTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JPAQueryFactory queryFactory;

    private ReportCustomRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ReportCustomRepositoryImpl(queryFactory);
    }

    @Nested
    @DisplayName("지점별 신고 조회는")
    class Describe_findByPlaces {

        @Test
        @DisplayName("신고자의 회원 상세 지점이 요청 지점에 있는 신고만 반환한다")
        void it_selects_reports_by_the_reporter_member_detail_place() {
            Place targetPlace = place("대상");
            Place otherPlace = place("다른");
            MemberJpaEntity targetReporter = memberAtPlace("대상신고자", targetPlace);
            MemberJpaEntity otherReporter = memberAtPlace("다른신고자", otherPlace);
            MemberJpaEntity targetPlaceReported = memberAtPlace("대상지점피신고자", targetPlace);
            MemberJpaEntity otherPlaceReported = memberAtPlace("다른지점피신고자", otherPlace);
            Report included = report(targetReporter, otherPlaceReported, "포함 신고");
            report(otherReporter, targetPlaceReported, "제외 신고");
            em.flush();
            em.clear();

            List<Report> result = repository.findByPlaces(List.of(targetPlace));

            assertThat(result).extracting(Report::getId).containsExactly(included.getId());
            assertThat(isLoaded(result.getFirst().getReporter())).isTrue();
            assertThat(isLoaded(result.getFirst().getReported())).isTrue();
        }
    }

    private Place place(String suffix) {
        Head head = em.persist(Head.builder().name("본부-" + suffix).build());
        return em.persist(Place.builder().name("지점-" + suffix).head(head).build());
    }

    private MemberJpaEntity memberAtPlace(String suffix, Place place) {
        MemberJpaEntity member = em.persist(MemberJpaEntity.builder()
                .name("이름-" + suffix)
                .nickname("회원-" + suffix)
                .password("pw")
                .phoneNumber("010-3000-" + String.format("%04d", suffix.hashCode() & 0x7fff))
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
        em.persist(MemberDetailJpaEntity.builder()
                .member(member)
                .dong(em.persist(DongJpaEntity.builder().name("동-" + suffix).build()))
                .place(place)
                .gwangsan(0)
                .light(0)
                .description("소개")
                .build());
        return member;
    }

    private Report report(MemberJpaEntity reporter, MemberJpaEntity reported, String content) {
        return em.persist(Report.builder()
                .reportType(ReportType.ETC)
                .content(content)
                .reporter(reporter)
                .reported(reported)
                .build());
    }

    private boolean isLoaded(Object association) {
        return em.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(association);
    }
}
