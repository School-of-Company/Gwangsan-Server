package team.startup.gwangsan.application.member.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.adapter.integration.member.LegacyMemberKeywordAdapter;
import team.startup.gwangsan.adapter.persistence.member.*;
import team.startup.gwangsan.application.member.data.MemberProfile;
import team.startup.gwangsan.application.member.data.MyInfoResult;
import team.startup.gwangsan.application.member.data.UserInfoResult;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.member.exception.NotAllowedUserAccessException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;
import team.startup.gwangsan.domain.relatedkeyword.repository.RelatedKeywordRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 회원 조회 서비스. 실제 persistence·keyword adapter 를 거쳐 기존 조회 메서드(fetch join 경로) 선택까지 확인한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("회원 조회 서비스 단위 테스트")
class MemberQueryServicesTest {

    private static final LocalDateTime JOINED_AT = LocalDateTime.of(2026, 1, 2, 3, 4);

    @Mock private MemberJpaRepository memberJpaRepository;
    @Mock private MemberDetailJpaRepository memberDetailJpaRepository;
    @Mock private WithdrawalRecordJpaRepository withdrawalRecordJpaRepository;
    @Mock private RelatedKeywordRepository relatedKeywordRepository;
    @Mock private MemberRelatedKeywordRepository memberRelatedKeywordRepository;
    @Mock private EntityManager entityManager;

    private FindMyInfoService findMyInfo;
    private FindUserInfoService findUserInfo;
    private FindAllUserInfoService findAllUserInfo;

    @BeforeEach
    void setUp() {
        MemberPersistenceAdapter persistence = new MemberPersistenceAdapter(memberJpaRepository,
                memberDetailJpaRepository, withdrawalRecordJpaRepository, entityManager);
        LegacyMemberKeywordAdapter keywords = new LegacyMemberKeywordAdapter(relatedKeywordRepository,
                memberRelatedKeywordRepository, entityManager);
        findMyInfo = new FindMyInfoService(persistence, keywords);
        findUserInfo = new FindUserInfoService(persistence, keywords);
        findAllUserInfo = new FindAllUserInfoService(persistence);
    }

    private MemberJpaEntity member(Long id, String nickname) {
        MemberJpaEntity member = mock(MemberJpaEntity.class);
        lenient().when(member.getId()).thenReturn(id);
        lenient().when(member.getNickname()).thenReturn(nickname);
        lenient().when(member.getName()).thenReturn("이름");
        lenient().when(member.getPhoneNumber()).thenReturn("01012345678");
        lenient().when(member.getRole()).thenReturn(MemberRole.ROLE_USER);
        lenient().when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);
        lenient().when(member.getJoinedAt()).thenReturn(JOINED_AT);
        return member;
    }

    private MemberDetailJpaEntity detail(MemberJpaEntity member, Place place, int light, int gwangsan, String description) {
        MemberDetailJpaEntity detail = mock(MemberDetailJpaEntity.class);
        lenient().when(detail.getMember()).thenReturn(member);
        lenient().when(detail.getPlace()).thenReturn(place);
        lenient().when(detail.getLight()).thenReturn(light);
        lenient().when(detail.getGwangsan()).thenReturn(gwangsan);
        lenient().when(detail.getDescription()).thenReturn(description);
        return detail;
    }

    private Place place(Integer id, String name, Head head) {
        Place place = mock(Place.class);
        lenient().when(place.getId()).thenReturn(id);
        lenient().when(place.getName()).thenReturn(name);
        lenient().when(place.getHead()).thenReturn(head);
        return place;
    }

    private void keywordsOf(Long memberId, MemberJpaEntity member, String name) {
        RelatedKeyword keyword = mock(RelatedKeyword.class);
        when(keyword.getName()).thenReturn(name);
        MemberRelatedKeyword mrk = mock(MemberRelatedKeyword.class);
        when(mrk.getRelatedKeyword()).thenReturn(keyword);
        when(entityManager.getReference(MemberJpaEntity.class, memberId)).thenReturn(member);
        when(memberRelatedKeywordRepository.findAllByMember(member)).thenReturn(List.of(mrk));
    }

    @Nested
    @DisplayName("FindMyInfoService.query() 는")
    class Describe_find_my_info {

        @Test
        @DisplayName("지점·본부·동을 조인한 경로로 내 정보를 반환한다")
        void it_returns_my_info() {
            Head head = mock(Head.class);
            when(head.getName()).thenReturn("광산구");
            Dong dong = mock(Dong.class);
            when(dong.getName()).thenReturn("수완동");
            MemberJpaEntity member = member(1L, "테스터일");
            MemberDetailJpaEntity detail = detail(member, place(5, "수완지점", head), 5, 1000, "자기소개");
            when(detail.getDong()).thenReturn(dong);
            when(memberDetailJpaRepository.findByMemberIdWithPlaceHeadDong(1L)).thenReturn(Optional.of(detail));
            keywordsOf(1L, member, "Java");

            MyInfoResult result = findMyInfo.query(new ActorContext(1L, ActorContext.Role.ROLE_USER));

            assertThat(result).isEqualTo(new MyInfoResult(1L, "테스터일", "수완지점", "광산구", "수완동", 5, 1000,
                    "자기소개", List.of("Java")));
        }

        @Test
        @DisplayName("상세 정보가 없으면 NotFoundMemberException을 던진다")
        void it_throws_not_found_member_exception() {
            when(memberDetailJpaRepository.findByMemberIdWithPlaceHeadDong(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> findMyInfo.query(new ActorContext(1L, ActorContext.Role.ROLE_USER)))
                    .isInstanceOf(NotFoundMemberException.class);
        }
    }

    @Nested
    @DisplayName("FindUserInfoService.query() 는")
    class Describe_find_user_info {

        @Test
        @DisplayName("회원·지점을 조인한 경로로 회원 정보를 반환한다")
        void it_returns_user_info() {
            MemberJpaEntity member = member(2L, "테스터이");
            MemberDetailJpaEntity detail = detail(member, place(5, "수완지점", null), 3, 500, "설명");
            when(memberDetailJpaRepository.findByMemberIdWithMemberAndPlace(2L)).thenReturn(Optional.of(detail));
            keywordsOf(2L, member, "Spring");

            UserInfoResult result = findUserInfo.query(2L);

            assertThat(result).isEqualTo(new UserInfoResult(2L, "테스터이", "수완지점", 3, 500, "설명",
                    List.of("Spring"), "이름", "01012345678", MemberRole.ROLE_USER, MemberStatus.ACTIVE, JOINED_AT));
            verify(detail, never()).getDong();
        }

        @Test
        @DisplayName("회원이 없으면 NotFoundMemberException을 던진다")
        void it_throws_not_found_member_exception() {
            when(memberDetailJpaRepository.findByMemberIdWithMemberAndPlace(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> findUserInfo.query(99L)).isInstanceOf(NotFoundMemberException.class);
        }
    }

    @Nested
    @DisplayName("FindAllUserInfoService.query() 는")
    class Describe_find_all_user_info {

        private void adminScope(Integer placeId, Integer headId) {
            Head head = mock(Head.class);
            when(head.getId()).thenReturn(headId);
            MemberDetailJpaEntity adminDetail = detail(member(1L, "관리자"), place(placeId, "지점", head), 1, 0, "");
            when(memberDetailJpaRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
        }

        @Test
        @DisplayName("ROLE_HEAD_ADMIN 이면 headId 로 전체 회원 목록을 반환한다")
        void it_filters_by_head_for_head_admin() {
            adminScope(5, 10);
            MemberDetailJpaEntity target = detail(member(2L, "회원A"), place(5, "지점", null), 1, 100, "");
            when(memberDetailJpaRepository.findAllByRoleAndNicknameAndPlaceName(null, 10, "회원A", null))
                    .thenReturn(List.of(target));

            List<MemberProfile> result = findAllUserInfo.query(
                    new ActorContext(1L, ActorContext.Role.ROLE_HEAD_ADMIN), "회원A", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).nickname()).isEqualTo("회원A");
            assertThat(result.get(0).gwangsan()).isEqualTo(100);
        }

        @Test
        @DisplayName("ROLE_PLACE_ADMIN 이면 placeId 로 전체 회원 목록을 반환한다")
        void it_filters_by_place_for_place_admin() {
            adminScope(5, 10);
            MemberDetailJpaEntity target = detail(member(3L, "회원B"), place(5, "지점", null), 1, 200, "");
            when(memberDetailJpaRepository.findAllByRoleAndNicknameAndPlaceName(5, null, null, "지점"))
                    .thenReturn(List.of(target));

            List<MemberProfile> result = findAllUserInfo.query(
                    new ActorContext(1L, ActorContext.Role.ROLE_PLACE_ADMIN), null, "지점");

            assertThat(result).extracting(MemberProfile::nickname).containsExactly("회원B");
        }

        @Test
        @DisplayName("ROLE_USER 면 상세 조회 후 NotAllowedUserAccessException을 던진다")
        void it_rejects_ordinary_member() {
            adminScope(5, 10);

            assertThatThrownBy(() -> findAllUserInfo.query(new ActorContext(1L, ActorContext.Role.ROLE_USER), null, null))
                    .isInstanceOf(NotAllowedUserAccessException.class);
            verify(memberDetailJpaRepository, never()).findAllByRoleAndNicknameAndPlaceName(any(), any(), any(), any());
        }

        @Test
        @DisplayName("관리자 상세가 없으면 역할 검사보다 먼저 NotFoundMemberException을 던진다")
        void it_throws_not_found_before_role_check() {
            when(memberDetailJpaRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> findAllUserInfo.query(new ActorContext(1L, ActorContext.Role.ROLE_USER), null, null))
                    .isInstanceOf(NotFoundMemberException.class);
        }
    }
}
