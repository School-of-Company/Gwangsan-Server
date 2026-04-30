package team.startup.gwangsan.domain.auth.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotCompletedException;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotFoundException;
import team.startup.gwangsan.domain.auth.presentation.dto.request.FindNicknameRequest;
import team.startup.gwangsan.domain.auth.presentation.dto.response.FindNicknameResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.redis.RedisUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindNicknameServiceImpl 단위 테스트")
class FindNicknameServiceImplTest {

    @InjectMocks
    private FindNicknameServiceImpl service;

    @Mock private MemberRepository memberRepository;
    @Mock private RedisUtil redisUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상적인 별칭 찾기 요청 시")
        class Context_with_valid_request {

            @Test
            @DisplayName("별칭을 반환하고 Redis 인증 키를 삭제한다")
            void it_returns_nickname_and_deletes_redis_key() {
                FindNicknameRequest request = new FindNicknameRequest("01012345678");
                Member member = mock(Member.class);

                when(redisUtil.get("sms:verified:01012345678", Boolean.class)).thenReturn(true);
                when(memberRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.of(member));
                when(member.getNickname()).thenReturn("테스트별칭");

                FindNicknameResponse response = service.execute(request);

                assertThat(response.nickname()).isEqualTo("테스트별칭");
                verify(redisUtil).delete("sms:verified:01012345678");
            }
        }

        @Nested
        @DisplayName("SMS 인증 정보가 Redis에 없을 때")
        class Context_with_sms_not_found {

            @Test
            @DisplayName("SmsAuthNotFoundException을 던진다")
            void it_throws_sms_not_found_exception() {
                FindNicknameRequest request = new FindNicknameRequest("01012345678");
                when(redisUtil.get("sms:verified:01012345678", Boolean.class)).thenReturn(null);

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(SmsAuthNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("SMS 인증이 완료되지 않았을 때")
        class Context_with_sms_not_completed {

            @Test
            @DisplayName("SmsAuthNotCompletedException을 던진다")
            void it_throws_sms_not_completed_exception() {
                FindNicknameRequest request = new FindNicknameRequest("01012345678");
                when(redisUtil.get("sms:verified:01012345678", Boolean.class)).thenReturn(false);

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(SmsAuthNotCompletedException.class);
            }
        }

        @Nested
        @DisplayName("회원이 존재하지 않을 때")
        class Context_with_member_not_found {

            @Test
            @DisplayName("NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                FindNicknameRequest request = new FindNicknameRequest("01099999999");
                when(redisUtil.get("sms:verified:01099999999", Boolean.class)).thenReturn(true);
                when(memberRepository.findByPhoneNumber("01099999999")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(request))
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }
    }
}
