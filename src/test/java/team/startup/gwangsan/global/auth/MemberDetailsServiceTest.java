package team.startup.gwangsan.global.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberDetailsService 단위 테스트")
class MemberDetailsServiceTest {

    private static final String PHONE_NUMBER = "010-1234-5678";

    @Mock private MemberRepository memberRepository;

    @Nested
    @DisplayName("loadUserByUsername()은")
    class Describe_loadUserByUsername {

        @Test
        @DisplayName("it_회원이 있으면 실제 MemberDetails를 반환한다")
        void it_returns_member_details_when_member_exists() {
            Member member = member();
            when(memberRepository.findByPhoneNumber(PHONE_NUMBER)).thenReturn(Optional.of(member));

            UserDetails userDetails = service().loadUserByUsername(PHONE_NUMBER);

            assertThat(userDetails).isInstanceOf(MemberDetails.class);
            MemberDetails memberDetails = (MemberDetails) userDetails;
            assertThat(memberDetails.getMember()).isSameAs(member);
            assertThat(memberDetails.getUsername()).isEqualTo(PHONE_NUMBER);
            assertThat(memberDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
            verify(memberRepository).findByPhoneNumber(PHONE_NUMBER);
        }

        @Test
        @DisplayName("it_회원이 없으면 NotFoundMemberException을 던진다")
        void it_throws_not_found_when_member_does_not_exist() {
            when(memberRepository.findByPhoneNumber(PHONE_NUMBER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().loadUserByUsername(PHONE_NUMBER))
                    .isInstanceOf(NotFoundMemberException.class);

            verify(memberRepository).findByPhoneNumber(PHONE_NUMBER);
        }
    }

    private MemberDetailsService service() {
        return new MemberDetailsService(memberRepository);
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
