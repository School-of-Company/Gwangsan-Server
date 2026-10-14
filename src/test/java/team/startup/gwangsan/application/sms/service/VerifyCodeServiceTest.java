package team.startup.gwangsan.application.sms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.adapter.redis.sms.SmsCodeRedisAdapter;
import team.startup.gwangsan.domain.sms.exception.NotFoundSmsAuthException;
import team.startup.gwangsan.domain.sms.exception.NotMatchRandomCodeException;
import team.startup.gwangsan.domain.common.exception.ErrorCode;
import team.startup.gwangsan.adapter.redis.RedisUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 가입·비밀번호 재설정·별칭 찾기 인증번호 확인(/api/sms/verify, /password/verify, /nickname/verify).
 * 세 기존 서비스의 로직이 같아 하나로 합쳤다. 기존 세 테스트의 시나리오를 모두 포함한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerifyCodeService / PhoneVerificationService 단위 테스트")
class VerifyCodeServiceTest {

    private static final long TEN_MINUTES = 10 * 60 * 1000L;

    @Mock private RedisUtil redisUtil;

    private VerifyCodeService service;
    private PhoneVerificationService phoneVerification;

    @BeforeEach
    void setUp() {
        SmsCodeRedisAdapter store = new SmsCodeRedisAdapter(redisUtil);
        service = new VerifyCodeService(store);
        phoneVerification = new PhoneVerificationService(store);
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("코드가 일치하면 인증 완료 키를 10분 저장하고 코드 키를 삭제한다")
        void it_saves_verified_key_and_deletes_code_key() {
            when(redisUtil.get("sms:code:01012345678", String.class)).thenReturn("123456");

            service.execute("01012345678", "123456");

            verify(redisUtil).set("sms:verified:01012345678", Boolean.TRUE, TEN_MINUTES);
            verify(redisUtil).delete("sms:code:01012345678");
        }

        @Test
        @DisplayName("데모 번호와 데모 코드면 인증 완료 키만 저장하고 코드 키는 삭제하지 않는다")
        void it_saves_verified_key_without_deleting_code_key() {
            service.execute("01011111111", "000000");

            verify(redisUtil).set("sms:verified:01011111111", Boolean.TRUE, TEN_MINUTES);
            verify(redisUtil, never()).delete(anyString());
            verify(redisUtil, never()).get(anyString(), any());
        }

        @Test
        @DisplayName("데모 번호에 잘못된 코드면 저장된 코드를 검증하고 인증 완료로 처리하지 않는다")
        void it_validates_saved_code_instead_of_auto_approving() {
            when(redisUtil.get("sms:code:01011111111", String.class)).thenReturn("123456");

            assertThrows(NotMatchRandomCodeException.class, () -> service.execute("01011111111", "999999"));
            verify(redisUtil, never()).set(eq("sms:verified:01011111111"), any(), anyLong());
            verify(redisUtil, never()).delete(anyString());
        }

        @Test
        @DisplayName("코드가 만료되었으면 기존과 같은 NOT_FOUND_SMS_AUTH 오류를 던진다")
        void it_throws_sms_auth_not_found_exception() {
            when(redisUtil.get("sms:code:01012345678", String.class)).thenReturn(null);

            NotFoundSmsAuthException e = assertThrows(NotFoundSmsAuthException.class,
                    () -> service.execute("01012345678", "123456"));
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_SMS_AUTH);
        }

        @Test
        @DisplayName("코드가 불일치하면 NotMatchRandomCodeException을 던진다")
        void it_throws_not_match_random_code_exception() {
            when(redisUtil.get("sms:code:01012345678", String.class)).thenReturn("123456");

            assertThrows(NotMatchRandomCodeException.class, () -> service.execute("01012345678", "999999"));
            verify(redisUtil, never()).set(anyString(), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("PhoneVerificationService 는")
    class Describe_phone_verification {

        @Test
        @DisplayName("sms:verified 키를 읽고 소비한다")
        void it_reads_and_consumes_verified_key() {
            when(redisUtil.get("sms:verified:01012345678", Boolean.class)).thenReturn(true);

            assertThat(phoneVerification.findVerified("01012345678")).contains(true);
            phoneVerification.consume("01012345678");

            verify(redisUtil).delete("sms:verified:01012345678");
            assertThat(phoneVerification.isDemoNumber("01011111111")).isTrue();
            assertThat(phoneVerification.isDemoNumber("01012345678")).isFalse();
        }
    }
}
