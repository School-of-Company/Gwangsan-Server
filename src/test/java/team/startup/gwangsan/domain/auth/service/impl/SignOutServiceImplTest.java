package team.startup.gwangsan.domain.auth.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.auth.repository.RefreshTokenRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.redis.RedisUtil;
import team.startup.gwangsan.global.security.jwt.JwtProvider;
import team.startup.gwangsan.global.util.MemberUtil;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignOutServiceImpl 단위 테스트")
class SignOutServiceImplTest {

    @InjectMocks
    private SignOutServiceImpl service;

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private RedisUtil redisUtil;
    @Mock private JwtProvider jwtProvider;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상 로그아웃 시")
        class Context_with_valid_request {

            @Test
            @DisplayName("refresh token을 삭제하고 access token을 블랙리스트에 등록한다")
            void it_deletes_refresh_token_and_blacklists_access_token() {
                Member member = mock(Member.class);
                when(member.getPhoneNumber()).thenReturn("01012345678");
                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(jwtProvider.getExpiration("accessToken")).thenReturn(300000L);

                service.execute("accessToken");

                verify(refreshTokenRepository).deleteById("01012345678");
                verify(redisUtil).setBlackList("accessToken", "BLACKLIST", 300000L);
            }
        }
    }
}
