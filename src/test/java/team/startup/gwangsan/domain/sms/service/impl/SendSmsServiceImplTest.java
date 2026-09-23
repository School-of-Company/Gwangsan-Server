package team.startup.gwangsan.domain.sms.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.sms.exception.AlreadyRegisteredPhoneNumberException;
import team.startup.gwangsan.domain.sms.exception.TooManyRequestAuthCodeException;
import team.startup.gwangsan.domain.sms.presentation.dto.SendSmsRequest;
import team.startup.gwangsan.global.redis.RedisUtil;
import team.startup.gwangsan.global.sms.SmsSendHelper;
import team.startup.gwangsan.global.sms.SmsProperties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendSmsServiceImpl 단위 테스트")
class SendSmsServiceImplTest {

    @InjectMocks
    private SendSmsServiceImpl service;

    @Mock
    private SmsSendHelper smsSendHelper;

    @Mock
    private SmsProperties smsProperties;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RedisUtil redisUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("데모 번호일 때")
        class Context_with_demo_phone_number {

            @Test
            @DisplayName("회원 조회·인증 정보 저장·SMS 발송 없이 즉시 반환한다")
            void it_returns_immediately_without_sending_sms() {
                service.execute(new SendSmsRequest("01011111111"));

                verifyNoInteractions(memberRepository, redisUtil, smsSendHelper);
            }
        }

        @Nested
        @DisplayName("정상적인 요청일 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("인증 정보를 저장하고 SMS를 비동기 발송한다")
            void it_saves_auth_info_and_sends_sms_async() {
                SendSmsRequest request = new SendSmsRequest("01012345678");

                when(memberRepository.existsByPhoneNumber("01012345678")).thenReturn(false);
                when(redisUtil.get("sms:attempt:01012345678", Integer.class)).thenReturn(0);
                when(smsProperties.getFromNumber()).thenReturn("01000000000");

                service.execute(request);

                verify(redisUtil).set(eq("sms:attempt:01012345678"), eq(1), anyLong());
                verify(redisUtil).set(eq("sms:code:01012345678"), anyString(), anyLong());
                verify(smsSendHelper).sendAsync(eq("01000000000"), eq("01012345678"), anyString());
            }
        }

        @Nested
        @DisplayName("이미 등록된 전화번호일 때")
        class Context_with_already_registered_phone_number {

            @Test
            @DisplayName("AlreadyRegisteredPhoneNumberException을 던진다")
            void it_throws_already_registered_phone_number_exception() {
                SendSmsRequest request = new SendSmsRequest("01012345678");

                when(memberRepository.existsByPhoneNumber("01012345678")).thenReturn(true);

                assertThrows(AlreadyRegisteredPhoneNumberException.class,
                        () -> service.execute(request));

                verifyNoInteractions(smsSendHelper);
            }
        }

        @Nested
        @DisplayName("시도 횟수가 5회 이상일 때")
        class Context_with_too_many_attempts {

            @Test
            @DisplayName("TooManyRequestAuthCodeException을 던진다")
            void it_throws_too_many_request_auth_code_exception() {
                SendSmsRequest request = new SendSmsRequest("01012345678");

                when(memberRepository.existsByPhoneNumber("01012345678")).thenReturn(false);
                when(redisUtil.get("sms:attempt:01012345678", Integer.class)).thenReturn(5);

                assertThrows(TooManyRequestAuthCodeException.class,
                        () -> service.execute(request));

                verifyNoInteractions(smsSendHelper);
            }
        }

        @Nested
        @DisplayName("시도 횟수 정보가 없을 때 (최초 요청)")
        class Context_with_null_attempt {

            @Test
            @DisplayName("시도 횟수를 1로 초기화하고 SMS를 발송한다")
            void it_initializes_attempt_and_sends_sms() {
                SendSmsRequest request = new SendSmsRequest("01099999999");

                when(memberRepository.existsByPhoneNumber("01099999999")).thenReturn(false);
                when(redisUtil.get("sms:attempt:01099999999", Integer.class)).thenReturn(null);
                when(smsProperties.getFromNumber()).thenReturn("01000000000");

                service.execute(request);

                verify(redisUtil).set(eq("sms:attempt:01099999999"), eq(1), anyLong());
                verify(smsSendHelper).sendAsync(eq("01000000000"), eq("01099999999"), anyString());
            }
        }
    }
}
