package team.startup.gwangsan.domain.sms.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotFoundException;
import team.startup.gwangsan.domain.sms.exception.NotMatchRandomCodeException;
import team.startup.gwangsan.domain.sms.presentation.dto.VerifyCodeRequest;
import team.startup.gwangsan.global.redis.RedisUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerifyResetPasswordCodeServiceImpl 단위 테스트")
class VerifyResetPasswordCodeServiceImplTest {

    @InjectMocks
    private VerifyResetPasswordCodeServiceImpl service;

    @Mock
    private RedisUtil redisUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("코드가 일치할 때")
        class Context_with_matching_code {

            @Test
            @DisplayName("인증 완료 키를 저장하고 코드 키를 삭제한다")
            void it_saves_verified_key_and_deletes_code_key() {
                VerifyCodeRequest request = new VerifyCodeRequest("01012345678", "123456");

                when(redisUtil.get("sms:code:01012345678", String.class)).thenReturn("123456");

                service.execute(request);

                verify(redisUtil).set(eq("sms:verified:01012345678"), eq(Boolean.TRUE), anyLong());
                verify(redisUtil).delete("sms:code:01012345678");
            }
        }

        @Nested
        @DisplayName("코드가 만료되었을 때")
        class Context_with_expired_code {

            @Test
            @DisplayName("SmsAuthNotFoundException을 던진다")
            void it_throws_sms_auth_not_found_exception() {
                VerifyCodeRequest request = new VerifyCodeRequest("01012345678", "123456");

                when(redisUtil.get("sms:code:01012345678", String.class)).thenReturn(null);

                assertThrows(SmsAuthNotFoundException.class,
                        () -> service.execute(request));
            }
        }

        @Nested
        @DisplayName("코드가 불일치할 때")
        class Context_with_mismatched_code {

            @Test
            @DisplayName("NotMatchRandomCodeException을 던진다")
            void it_throws_not_match_random_code_exception() {
                VerifyCodeRequest request = new VerifyCodeRequest("01012345678", "999999");

                when(redisUtil.get("sms:code:01012345678", String.class)).thenReturn("123456");

                assertThrows(NotMatchRandomCodeException.class,
                        () -> service.execute(request));
            }
        }
    }
}