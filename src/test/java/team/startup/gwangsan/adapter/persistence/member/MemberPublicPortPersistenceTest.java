package team.startup.gwangsan.adapter.persistence.member;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.application.member.port.in.AddMemberLightUseCase;
import team.startup.gwangsan.application.member.port.in.ManageMemberUseCase;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.application.member.service.AddMemberLightService;
import team.startup.gwangsan.application.member.service.ManageMemberService;
import team.startup.gwangsan.application.member.service.MemberAccountService;
import team.startup.gwangsan.application.member.service.MemberSummaryService;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

/** member 공개 Port 의 실제 JPA 쿼리 결과·쿼리 수·명시 저장을 확인한다. 테스트 트랜잭션이 호출자 트랜잭션 역할을 한다. */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import({QueryDslConfig.class, MemberPersistenceAdapter.class, MemberAccountService.class, MemberSummaryService.class,
        ManageMemberService.class, AddMemberLightService.class})
@DisplayName("member 공개 Port 영속성 통합 테스트")
class MemberPublicPortPersistenceTest {

    @Autowired private TestEntityManager em;
    @Autowired private MemberAccountQuery memberAccountQuery;
    @Autowired private MemberSummaryQuery memberSummaryQuery;
    @Autowired private ManageMemberUseCase manageMemberUseCase;
    @Autowired private AddMemberLightUseCase addMemberLightUseCase;

    private DongJpaEntity dong;
    private Place place;
    private Place otherPlace;

    @BeforeEach
    void setUp() {
        dong = em.persist(DongJpaEntity.builder().name("테스트동").build());
        Head head = em.persist(new Head("본부"));
        place = em.persist(Place.builder().name("지점").head(head).build());
        otherPlace = em.persist(Place.builder().name("다른지점").head(head).build());
    }

    @Nested
    @DisplayName("조회 Port 는")
    class Describe_queries {

        @Test
        @DisplayName("it_회원 기준 left join 1 쿼리로 요약을 주고 상세가 없으면 상세 값을 null 로 둔다")
        void it_returns_summaries_in_one_query() {
            MemberJpaEntity recommender = member("추천인", "01000000001", null);
            MemberJpaEntity withDetail = member("상세있음", "01000000002", recommender);
            detail(withDetail, place, 42);
            MemberJpaEntity withoutDetail = member("상세없음", "01000000003", null);
            Statistics statistics = clearAndStatistics();

            List<MemberSummary> result = memberSummaryQuery.findAllByIds(
                    List.of(withDetail.getId(), withoutDetail.getId(), 999_999L));

            assertThat(result).extracting(MemberSummary::memberId, MemberSummary::nickname, MemberSummary::phoneNumber,
                            MemberSummary::recommenderId, MemberSummary::recommenderNickname, MemberSummary::placeId,
                            MemberSummary::headId, MemberSummary::placeName, MemberSummary::light, MemberSummary::hasDetail)
                    .containsExactlyInAnyOrder(
                            tuple(withDetail.getId(), "상세있음", "01000000002", recommender.getId(), "추천인",
                                    place.getId(), place.getHead().getId(), "지점", 42, true),
                            tuple(withoutDetail.getId(), "상세없음", "01000000003", null, null,
                                    null, null, null, null, false));
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("it_빈 입력이면 쿼리를 보내지 않는다")
        void it_skips_query_for_empty_input() {
            Statistics statistics = clearAndStatistics();

            assertThat(memberSummaryQuery.findAllByIds(List.of())).isEmpty();
            assertThat(memberAccountQuery.findExistingIds(List.of())).isEmpty();
            assertThat(statistics.getPrepareStatementCount()).isZero();
        }

        @Test
        @DisplayName("it_단건 조회는 1 쿼리이고 없는 회원이면 empty 다")
        void it_finds_one_summary() {
            MemberJpaEntity member = member("단건", "01000000004", null);
            detail(member, place, 7);
            Statistics statistics = clearAndStatistics();

            assertThat(memberSummaryQuery.findById(member.getId())).get()
                    .extracting(MemberSummary::placeId, MemberSummary::light).containsExactly(place.getId(), 7);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
            assertThat(memberSummaryQuery.findById(999_999L)).isEmpty();
        }

        @Test
        @DisplayName("it_존재 회원 ID 와 지점 회원 ID 를 각각 1 쿼리로 준다")
        void it_finds_ids_in_one_query_each() {
            MemberJpaEntity first = member("첫째", "01000000005", null);
            detail(first, place, 1);
            MemberJpaEntity second = member("둘째", "01000000006", null);
            detail(second, otherPlace, 1);
            Statistics statistics = clearAndStatistics();

            assertThat(memberAccountQuery.findExistingIds(List.of(first.getId(), second.getId(), 999_999L)))
                    .containsExactlyInAnyOrder(first.getId(), second.getId());
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
            assertThat(memberSummaryQuery.findMemberIdsInPlace(place.getId())).containsExactly(first.getId());
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("변경 Port 는")
    class Describe_commands {

        @Test
        @DisplayName("it_상태를 바꾸면 flush 후 DB 에 반영되고 없는 회원이면 NotFoundMember 다")
        void it_changes_status() {
            MemberJpaEntity member = member("정지대상", "01000000007", null);
            em.flush();
            em.clear();

            manageMemberUseCase.changeStatus(member.getId(), MemberStatus.SUSPENDED);
            em.flush();
            em.clear();

            assertThat(em.find(MemberJpaEntity.class, member.getId()).getStatus()).isEqualTo(MemberStatus.SUSPENDED);
            assertThatThrownBy(() -> manageMemberUseCase.changeStatus(999_999L, MemberStatus.ACTIVE))
                    .isInstanceOf(NotFoundMemberException.class);
        }

        @Test
        @DisplayName("it_지점과 역할을 2 쿼리로 읽어 바꾸고 상세가 없으면 NotFoundMemberDetail 이다")
        void it_changes_role_and_place() {
            MemberJpaEntity member = member("관리자후보", "01000000008", null);
            detail(member, place, 1);
            MemberJpaEntity noDetail = member("상세없음", "01000000009", null);
            Statistics statistics = clearAndStatistics();

            manageMemberUseCase.changeRole(member.getId(), MemberRole.ROLE_PLACE_ADMIN, otherPlace.getId());
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
            em.flush();
            em.clear();

            assertThat(em.find(MemberJpaEntity.class, member.getId()).getRole()).isEqualTo(MemberRole.ROLE_PLACE_ADMIN);
            assertThat(em.find(MemberDetailJpaEntity.class, member.getId()).getPlace().getId()).isEqualTo(otherPlace.getId());
            assertThatThrownBy(() -> manageMemberUseCase.changeRole(noDetail.getId(), MemberRole.ROLE_HEAD_ADMIN, place.getId()))
                    .isInstanceOf(NotFoundMemberDetailException.class);
        }

        @Test
        @DisplayName("it_가입 기각은 상세와 회원을 함께 지운다")
        void it_rejects_sign_up() {
            MemberJpaEntity member = member("기각", "01000000010", null);
            detail(member, place, 1);
            em.flush();
            em.clear();

            manageMemberUseCase.rejectSignUp(member.getId());
            em.flush();
            em.clear();

            assertThat(em.find(MemberDetailJpaEntity.class, member.getId())).isNull();
            assertThat(em.find(MemberJpaEntity.class, member.getId())).isNull();
        }

        @Test
        @DisplayName("it_탈퇴 기록을 지우고 남긴 광산을 돌려준다")
        void it_takes_withdrawn_gwangsan() {
            em.persist(WithdrawalRecordJpaEntity.builder().phoneNumber("01099999999").gwangsan(3000).banned(false).build());
            em.flush();
            em.clear();

            assertThat(manageMemberUseCase.takeWithdrawnGwangsan("01099999999")).contains(3000);
            em.flush();
            assertThat(manageMemberUseCase.takeWithdrawnGwangsan("01099999999")).isEmpty();
        }

        @Test
        @DisplayName("it_빛을 더하고 100 에서 멈춘다")
        void it_adds_light() {
            MemberJpaEntity member = member("빛", "01000000011", null);
            detail(member, place, 95);
            em.flush();
            em.clear();

            addMemberLightUseCase.execute(member.getId(), 10);
            em.flush();
            em.clear();

            assertThat(em.find(MemberDetailJpaEntity.class, member.getId()).getLight()).isEqualTo(100);
        }
    }

    private MemberJpaEntity member(String nickname, String phoneNumber, MemberJpaEntity recommender) {
        return em.persist(MemberJpaEntity.builder().name(nickname).nickname(nickname).password("pw")
                .phoneNumber(phoneNumber).recommender(recommender)
                .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build());
    }

    private void detail(MemberJpaEntity member, Place place, int light) {
        em.persist(MemberDetailJpaEntity.builder().member(member).dong(dong).place(place)
                .gwangsan(0).light(light).description("소개").build());
    }

    private Statistics clearAndStatistics() {
        em.flush();
        em.clear();
        Statistics statistics = em.getEntityManager().getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }
}
