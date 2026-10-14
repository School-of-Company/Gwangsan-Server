package team.startup.gwangsan.application.sms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.adapter.redis.sms.SmsCodeRedisAdapter;
import team.startup.gwangsan.application.sms.port.out.RegisteredPhonePort;
import team.startup.gwangsan.application.sms.port.out.SmsSenderPort;
import team.startup.gwangsan.domain.sms.exception.AlreadyRegisteredPhoneNumberException;
import team.startup.gwangsan.domain.sms.exception.NotRegisteredPhoneNumberException;
import team.startup.gwangsan.domain.sms.exception.SmsSendFailedException;
import team.startup.gwangsan.domain.sms.exception.TooManyRequestAuthCodeException;
import team.startup.gwangsan.adapter.redis.RedisUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 세 인증번호 발송 서비스. 저장은 실제 Redis adapter 를 거쳐 기존 키·TTL 계약을 함께 확인한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SMS 발송 서비스 단위 테스트")
class SendSmsServicesTest {

    private static final long THREE_MINUTES = 3 * 60 * 1000L;

    @Mock private RegisteredPhonePort registeredPhonePort;
    @Mock private RedisUtil redisUtil;
    @Mock private SmsSenderPort smsSenderPort;

    private SendSmsService signUp;
    private SendResetPasswordSmsService resetPassword;
    private SendFindNicknameSmsService findNickname;

    @BeforeEach
    void setUp() {
        SmsCodeRedisAdapter store = new SmsCodeRedisAdapter(redisUtil);
        signUp = new SendSmsService(registeredPhonePort, store, smsSenderPort);
        resetPassword = new SendResetPasswordSmsService(registeredPhonePort, store, smsSenderPort);
        findNickname = new SendFindNicknameSmsService(registeredPhonePort, store, smsSenderPort);
    }

    @Nested
    @DisplayName("데모 번호일 때 세 서비스 모두")
    class Context_with_demo_phone_number {

        @Test
        @DisplayName("회원 조회·인증 정보 저장·SMS 발송 없이 즉시 반환한다")
        void it_returns_immediately_without_sending_sms() {
            signUp.execute("01011111111");
            resetPassword.execute("01011111111");
            findNickname.execute("01011111111");

            verifyNoInteractions(registeredPhonePort, redisUtil, smsSenderPort);
        }
    }

    @Nested
    @DisplayName("SendSmsService(가입) 는")
    class Describe_sign_up {

        @Test
        @DisplayName("시도 횟수와 코드를 3분 TTL 로 저장하고 SMS를 비동기 발송한다")
        void it_saves_auth_info_and_sends_sms_async() {
            when(registeredPhonePort.isRegistered("01012345678")).thenReturn(false);
            when(redisUtil.get("sms:attempt:01012345678", Integer.class)).thenReturn(0);

            signUp.execute("01012345678");

            verify(redisUtil).set("sms:attempt:01012345678", 1, THREE_MINUTES);
            verify(redisUtil).set(eq("sms:code:01012345678"), matches("\\d{6}"), eq(THREE_MINUTES));
            verify(smsSenderPort).sendAsync(eq("01012345678"), startsWith("[시민화폐광산] 인증번호는 "));
        }

        @Test
        @DisplayName("이미 등록된 전화번호면 AlreadyRegisteredPhoneNumberException을 던진다")
        void it_throws_already_registered_phone_number_exception() {
            when(registeredPhonePort.isRegistered("01012345678")).thenReturn(true);

            assertThrows(AlreadyRegisteredPhoneNumberException.class, () -> signUp.execute("01012345678"));
            verifyNoInteractions(smsSenderPort, redisUtil);
        }

        @Test
        @DisplayName("시도 횟수가 5회 이상이면 TooManyRequestAuthCodeException을 던지고 아무것도 저장하지 않는다")
        void it_throws_too_many_request_auth_code_exception() {
            when(registeredPhonePort.isRegistered("01012345678")).thenReturn(false);
            when(redisUtil.get("sms:attempt:01012345678", Integer.class)).thenReturn(5);

            assertThrows(TooManyRequestAuthCodeException.class, () -> signUp.execute("01012345678"));
            verify(redisUtil, never()).set(anyString(), any(), anyLong());
            verifyNoInteractions(smsSenderPort);
        }

        @Test
        @DisplayName("시도 횟수 정보가 없으면(최초 요청) 1로 초기화하고 SMS를 발송한다")
        void it_initializes_attempt_and_sends_sms() {
            when(registeredPhonePort.isRegistered("01099999999")).thenReturn(false);
            when(redisUtil.get("sms:attempt:01099999999", Integer.class)).thenReturn(null);

            signUp.execute("01099999999");

            verify(redisUtil).set("sms:attempt:01099999999", 1, THREE_MINUTES);
            verify(smsSenderPort).sendAsync(eq("01099999999"), anyString());
        }
    }

    @Nested
    @DisplayName("SendResetPasswordSmsService 는")
    class Describe_reset_password {

        @Test
        @DisplayName("인증 정보를 저장하고 SMS를 비동기 발송한다")
        void it_saves_auth_info_and_sends_sms_async() {
            when(registeredPhonePort.isRegistered("01012345678")).thenReturn(true);
            when(redisUtil.get("sms:attempt:01012345678", Integer.class)).thenReturn(0);

            resetPassword.execute("01012345678");

            verify(redisUtil).set("sms:attempt:01012345678", 1, THREE_MINUTES);
            verify(redisUtil).set(eq("sms:code:01012345678"), anyString(), eq(THREE_MINUTES));
            verify(smsSenderPort).sendAsync(eq("01012345678"), startsWith("[시민화폐광산] 비밀번호 재설정 인증번호는 "));
        }

        @Test
        @DisplayName("미등록 전화번호면 NotRegisteredPhoneNumberException을 던진다")
        void it_throws_not_registered_phone_number_exception() {
            when(registeredPhonePort.isRegistered("01012345678")).thenReturn(false);

            assertThrows(NotRegisteredPhoneNumberException.class, () -> resetPassword.execute("01012345678"));
            verifyNoInteractions(smsSenderPort);
        }

        @Test
        @DisplayName("시도 횟수가 5회 이상이면 TooManyRequestAuthCodeException을 던진다")
        void it_throws_too_many_request_auth_code_exception() {
            when(registeredPhonePort.isRegistered("01012345678")).thenReturn(true);
            when(redisUtil.get("sms:attempt:01012345678", Integer.class)).thenReturn(5);

            assertThrows(TooManyRequestAuthCodeException.class, () -> resetPassword.execute("01012345678"));
            verifyNoInteractions(smsSenderPort);
        }
    }

    @Nested
    @DisplayName("SendFindNicknameSmsService 는")
    class Describe_find_nickname {

        @Test
        @DisplayName("시도 횟수를 올리고 동기 발송에 성공한 뒤에 코드를 저장한다")
        void it_sends_sms_and_saves_auth_code() {
            when(registeredPhonePort.isRegistered("01012345678")).thenReturn(true);
            when(redisUtil.get("sms:attempt:01012345678", Integer.class)).thenReturn(null);

            findNickname.execute("01012345678");

            InOrder order = inOrder(redisUtil, smsSenderPort);
            order.verify(redisUtil).set("sms:attempt:01012345678", 1, THREE_MINUTES);
            order.verify(smsSenderPort).send(eq("01012345678"), startsWith("[시민화폐광산] 별칭 찾기 인증번호는 "));
            order.verify(redisUtil).set(eq("sms:code:01012345678"), anyString(), eq(THREE_MINUTES));
            verify(smsSenderPort, never()).sendAsync(any(), any());
        }

        @Test
        @DisplayName("발송에 실패하면 SmsSendFailedException을 던지고 인증 코드를 저장하지 않는다")
        void it_throws_sms_send_failed_exception_without_saving_auth_code() {
            when(registeredPhonePort.isRegistered("01012345678")).thenReturn(true);
            when(redisUtil.get("sms:attempt:01012345678", Integer.class)).thenReturn(0);
            doThrow(new SmsSendFailedException()).when(smsSenderPort).send(eq("01012345678"), anyString());

            assertThrows(SmsSendFailedException.class, () -> findNickname.execute("01012345678"));
            verify(redisUtil).set("sms:attempt:01012345678", 1, THREE_MINUTES);
            verify(redisUtil, never()).set(eq("sms:code:01012345678"), anyString(), anyLong());
        }

        @Test
        @DisplayName("미등록 전화번호면 NotRegisteredPhoneNumberException을 던진다")
        void it_throws_not_registered_phone_number_exception() {
            when(registeredPhonePort.isRegistered("01012345678")).thenReturn(false);

            assertThrows(NotRegisteredPhoneNumberException.class, () -> findNickname.execute("01012345678"));
            verifyNoInteractions(smsSenderPort);
        }

        @Test
        @DisplayName("시도 횟수가 5회 이상이면 발송하지 않는다")
        void it_throws_too_many_request_auth_code_exception() {
            when(registeredPhonePort.isRegistered("01012345678")).thenReturn(true);
            when(redisUtil.get("sms:attempt:01012345678", Integer.class)).thenReturn(5);

            assertThrows(TooManyRequestAuthCodeException.class, () -> findNickname.execute("01012345678"));
            verifyNoInteractions(smsSenderPort);
            assertThat(mockingDetails(redisUtil).getInvocations()).hasSize(1);
        }
    }
}
