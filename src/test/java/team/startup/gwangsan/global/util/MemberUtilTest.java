package team.startup.gwangsan.global.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.auth.MemberDetails;
import team.startup.gwangsan.global.security.exception.InvalidMemberPrincipalException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberUtil 단위 테스트")
class MemberUtilTest {

    private static final String PHONE_NUMBER = "010-1234-5678";

    @Mock private MemberRepository memberRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentMember()는")
    class Describe_getCurrentMember {

        @Test
        @DisplayName("it_MemberDetails principal의 전화번호로 현재 회원을 반환한다")
        void it_returns_member_for_member_details_principal() {
            Member member = member();
            authenticate(new MemberDetails(member));
            when(memberRepository.findByPhoneNumber(PHONE_NUMBER)).thenReturn(Optional.of(member));

            assertThat(memberUtil().getCurrentMember()).isSameAs(member);

            verify(memberRepository).findByPhoneNumber(PHONE_NUMBER);
        }

        @Test
        @DisplayName("it_MemberDetails principal에 대응하는 회원이 없으면 NotFoundMemberException을 던진다")
        void it_throws_not_found_when_authenticated_member_was_removed() {
            authenticate(new MemberDetails(member()));
            when(memberRepository.findByPhoneNumber(PHONE_NUMBER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> memberUtil().getCurrentMember())
                    .isInstanceOf(NotFoundMemberException.class);

            verify(memberRepository).findByPhoneNumber(PHONE_NUMBER);
        }

        @Test
        @DisplayName("it_MemberDetails가 아닌 principal이면 저장소 조회 없이 InvalidMemberPrincipalException을 던진다")
        void it_rejects_wrong_principal_type_without_repository_lookup() {
            authenticate("anonymousUser");

            assertThatThrownBy(() -> memberUtil().getCurrentMember())
                    .isInstanceOf(InvalidMemberPrincipalException.class);

            verifyNoInteractions(memberRepository);
        }
    }

    private MemberUtil memberUtil() {
        return new MemberUtil(memberRepository);
    }

    private void authenticate(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null)
        );
    }

    private Member member() {
        return Member.builder()
                .name("회원")
                .nickname("회원닉네임")
                .phoneNumber(PHONE_NUMBER)
                .password("password")
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build();
    }
}
