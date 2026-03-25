package team.startup.gwangsan.domain.auth.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.auth.presentation.dto.response.MemberInfoResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenAuthenticationServiceImpl 단위 테스트")
class TokenAuthenticationServiceImplTest {

    @InjectMocks
    private TokenAuthenticationServiceImpl service;

    @Mock
    private MemberUtil memberUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("인증된 사용자가 있을 때")
        class Context_with_authenticated_member {

            @Test
            @DisplayName("현재 회원의 id와 nickname을 반환한다")
            void it_returns_member_info() {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(1L);
                when(member.getNickname()).thenReturn("테스터일");
                when(memberUtil.getCurrentMember()).thenReturn(member);

                MemberInfoResponse response = service.execute();

                assertThat(response.memberId()).isEqualTo(1L);
                assertThat(response.nickname()).isEqualTo("테스터일");
            }
        }
    }
}
