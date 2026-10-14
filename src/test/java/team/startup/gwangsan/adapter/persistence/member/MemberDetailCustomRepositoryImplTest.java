package team.startup.gwangsan.adapter.persistence.member;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.adapter.persistence.place.HeadJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;
import team.startup.gwangsan.global.querydsl.QueryDslConfig;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(QueryDslConfig.class)
@DisplayName("MemberDetailCustomRepositoryImpl H2 슬라이스 통합 테스트")
class MemberDetailCustomRepositoryImplTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JPAQueryFactory queryFactory;

    private MemberDetailCustomRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new MemberDetailCustomRepositoryImpl(queryFactory);
    }

    @Test
    @DisplayName("회원 ID로 지점을 찾고 요청한 회원만 지점명 맵에 담는다")
    void it_finds_a_place_and_maps_only_requested_member_ids() {
        PlaceJpaEntity firstPlace = place("첫", "A");
        PlaceJpaEntity secondPlace = place("둘", "B");
        MemberJpaEntity memberWithoutDetail = member("상세없음");
        MemberDetailJpaEntity first = detail("첫회원", firstPlace);
        MemberDetailJpaEntity second = detail("둘회원", secondPlace);
        em.flush();
        em.clear();

        assertThat(first.getMember().getId()).isNotEqualTo(memberWithoutDetail.getId());
        assertThat(first.getId()).isEqualTo(first.getMember().getId());
        assertThatThrownBy(() -> repository.findPlaceByMemberId(memberWithoutDetail.getId()))
                .isInstanceOf(NotFoundMemberException.class);
        assertThat(repository.findPlaceByMemberId(first.getMember().getId()).getId()).isEqualTo(firstPlace.getId());
        assertThat(repository.findPlaceNameMapByMemberIds(Set.of(first.getMember().getId(), second.getMember().getId(), 999L)))
                .isEqualTo(Map.of(first.getMember().getId(), firstPlace.getName(), second.getMember().getId(), secondPlace.getName()));
    }

    @Test
    @DisplayName("회원 목록은 place ID를 head ID보다 우선하고 별명과 지점명을 함께 제한한다")
    void it_scopes_members_by_place_head_nickname_and_place_name() {
        HeadJpaEntity firstHead = head("A");
        PlaceJpaEntity firstPlace = place("첫", firstHead);
        PlaceJpaEntity secondPlace = place("둘", firstHead);
        PlaceJpaEntity thirdPlace = place("셋", head("B"));
        MemberDetailJpaEntity first = detail("첫회원", firstPlace);
        MemberDetailJpaEntity second = detail("둘회원", secondPlace);
        MemberDetailJpaEntity third = detail("셋회원", thirdPlace);
        em.flush();
        em.clear();

        assertThat(repository.findAllByRoleAndNicknameAndPlaceName(null, firstPlace.getHead().getId(), null, null))
                .extracting(MemberDetailJpaEntity::getId).containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(repository.findAllByRoleAndNicknameAndPlaceName(firstPlace.getId(), thirdPlace.getHead().getId(), null, null))
                .extracting(MemberDetailJpaEntity::getId).containsExactly(first.getId());
        assertThat(repository.findAllByRoleAndNicknameAndPlaceName(null, null, "별명-셋회원", thirdPlace.getName()))
                .extracting(MemberDetailJpaEntity::getId).containsExactly(third.getId());
        assertThat(repository.findAllByRoleAndNicknameAndPlaceName(null, null, null, null))
                .extracting(MemberDetailJpaEntity::getId).containsExactlyInAnyOrder(first.getId(), second.getId(), third.getId());
    }

    @Test
    @DisplayName("상세 단건 조회는 요청한 관계를 fetch join으로 초기화한다")
    void it_fetches_requested_member_place_head_and_dong_associations() {
        PlaceJpaEntity place = place("대상", "본부");
        MemberDetailJpaEntity persisted = detail("대상회원", place);
        Long memberId = persisted.getMember().getId();
        String phoneNumber = persisted.getMember().getPhoneNumber();
        em.flush();
        em.clear();

        MemberDetailJpaEntity byMember = repository.findByMemberIdWithMember(memberId);
        assertThat(isLoaded(byMember.getMember())).isTrue();
        MemberDetailJpaEntity byPhone = repository.findByPhoneNumberWithMember(phoneNumber);
        assertThat(isLoaded(byPhone.getMember())).isTrue();
        MemberDetailJpaEntity withAll = repository.findByMemberIdWithPlaceHeadDong(memberId).orElseThrow();
        assertThat(isLoaded(withAll.getMember())).isTrue();
        assertThat(isLoaded(withAll.getPlace())).isTrue();
        assertThat(isLoaded(withAll.getPlace().getHead())).isTrue();
        assertThat(isLoaded(withAll.getDong())).isTrue();
        MemberDetailJpaEntity withPlace = repository.findByMemberIdWithMemberAndPlace(memberId).orElseThrow();
        assertThat(isLoaded(withPlace.getMember())).isTrue();
        assertThat(isLoaded(withPlace.getPlace())).isTrue();
    }

    @Test
    @DisplayName("없는 회원은 필수 단건 조회에서 예외이고 Optional 단건 조회에서는 비어 있다")
    void it_distinguishes_missing_required_and_optional_lookups() {
        assertThatThrownBy(() -> repository.findPlaceByMemberId(999L)).isInstanceOf(NotFoundMemberException.class);
        assertThatThrownBy(() -> repository.findByMemberIdWithMember(999L)).isInstanceOf(NotFoundMemberException.class);
        assertThatThrownBy(() -> repository.findByPhoneNumberWithMember("010-9999-9999"))
                .isInstanceOf(NotFoundMemberException.class);
        assertThat(repository.findByMemberIdWithPlaceHeadDong(999L)).isEmpty();
        assertThat(repository.findByMemberIdWithMemberAndPlace(999L)).isEmpty();
    }

    private MemberDetailJpaEntity detail(String suffix, PlaceJpaEntity place) {
        MemberJpaEntity member = member(suffix);
        return em.persist(MemberDetailJpaEntity.builder()
                .member(member)
                .dong(em.persist(DongJpaEntity.builder().name("동-" + suffix).build()))
                .place(place)
                .gwangsan(0)
                .light(0)
                .description("소개")
                .build());
    }

    private MemberJpaEntity member(String suffix) {
        return em.persist(MemberJpaEntity.builder()
                .name("회원-" + suffix)
                .nickname("별명-" + suffix)
                .phoneNumber("010-1000-" + String.format("%04d", suffix.hashCode() & 0x7fff))
                .password("pw")
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build());
    }

    private PlaceJpaEntity place(String suffix, String headSuffix) {
        return place(suffix, head(headSuffix));
    }

    private HeadJpaEntity head(String suffix) {
        return em.persist(HeadJpaEntity.builder().name("본부-" + suffix).build());
    }

    private PlaceJpaEntity place(String suffix, HeadJpaEntity head) {
        return em.persist(PlaceJpaEntity.builder().name("지점-" + suffix).head(head).build());
    }

    private boolean isLoaded(Object association) {
        return em.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(association);
    }
}
