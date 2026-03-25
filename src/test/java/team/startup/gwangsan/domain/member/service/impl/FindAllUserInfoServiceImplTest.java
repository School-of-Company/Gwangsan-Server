package team.startup.gwangsan.domain.member.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.exception.NotAllowedUserAccessException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindAllUserInfoResponse;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindAllUserInfoServiceImpl 단위 테스트")
class FindAllUserInfoServiceImplTest {

    @InjectMocks
    private FindAllUserInfoServiceImpl service;

    @Mock private MemberDetailRepository memberDetailRepository;
    @Mock private MemberUtil memberUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("ROLE_HEAD_ADMIN일 때")
        class Context_with_head_admin {

            @Test
            @DisplayName("headId로 전체 회원 목록을 반환한다")
            void it_returns_members_by_head_id() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                when(admin.getRole()).thenReturn(MemberRole.ROLE_HEAD_ADMIN);

                Head head = mock(Head.class);
                when(head.getId()).thenReturn(10);

                Place place = mock(Place.class);
                when(place.getHead()).thenReturn(head);

                MemberDetail adminDetail = mock(MemberDetail.class);
                when(adminDetail.getPlace()).thenReturn(place);

                Member targetMember = mock(Member.class);
                when(targetMember.getId()).thenReturn(2L);
                when(targetMember.getNickname()).thenReturn("회원A");

                MemberDetail targetDetail = mock(MemberDetail.class);
                when(targetDetail.getMember()).thenReturn(targetMember);
                when(targetDetail.getGwangsan()).thenReturn(100);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(memberDetailRepository.findAllByRoleAndNicknameAndPlaceName(null, 10, null, null))
                        .thenReturn(List.of(targetDetail));

                List<FindAllUserInfoResponse> result = service.execute(null, null);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).nickname()).isEqualTo("회원A");
            }
        }

        @Nested
        @DisplayName("ROLE_PLACE_ADMIN일 때")
        class Context_with_place_admin {

            @Test
            @DisplayName("placeId로 전체 회원 목록을 반환한다")
            void it_returns_members_by_place_id() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                when(admin.getRole()).thenReturn(MemberRole.ROLE_PLACE_ADMIN);

                Place place = mock(Place.class);
                when(place.getId()).thenReturn(5);

                MemberDetail adminDetail = mock(MemberDetail.class);
                when(adminDetail.getPlace()).thenReturn(place);

                Member targetMember = mock(Member.class);
                when(targetMember.getNickname()).thenReturn("회원B");

                MemberDetail targetDetail = mock(MemberDetail.class);
                when(targetDetail.getMember()).thenReturn(targetMember);
                when(targetDetail.getGwangsan()).thenReturn(200);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(memberDetailRepository.findAllByRoleAndNicknameAndPlaceName(5, null, "회원B", null))
                        .thenReturn(List.of(targetDetail));

                List<FindAllUserInfoResponse> result = service.execute("회원B", null);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).nickname()).isEqualTo("회원B");
            }
        }

        @Nested
        @DisplayName("ROLE_USER가 접근할 때")
        class Context_with_role_user {

            @Test
            @DisplayName("NotAllowedUserAccessException을 던진다")
            void it_throws_not_allowed_exception() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                when(admin.getRole()).thenReturn(MemberRole.ROLE_USER);

                MemberDetail adminDetail = mock(MemberDetail.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));

                assertThatThrownBy(() -> service.execute(null, null))
                        .isInstanceOf(NotAllowedUserAccessException.class);
            }
        }

        @Nested
        @DisplayName("MemberDetail이 없을 때")
        class Context_with_detail_not_found {

            @Test
            @DisplayName("NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(null, null))
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }
    }
}
