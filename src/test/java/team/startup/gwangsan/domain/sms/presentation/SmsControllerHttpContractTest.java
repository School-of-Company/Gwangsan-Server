package team.startup.gwangsan.domain.sms.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.domain.sms.exception.AlreadyRegisteredPhoneNumberException;
import team.startup.gwangsan.domain.sms.service.*;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SMS HTTP 계약")
class SmsControllerHttpContractTest {
    private MockMvc mvc;
    private SendSmsService sendSms;
    private VerifyCodeService verifyCode;
    private SendResetPasswordSmsService sendResetPasswordSms;
    private VerifyResetPasswordCodeService verifyResetPasswordCode;
    private SendFindNicknameSmsService sendFindNicknameSms;
    private VerifyFindNicknameCodeService verifyFindNicknameCode;

    @BeforeEach
    void setUp() {
        sendSms = mock(SendSmsService.class);
        verifyCode = mock(VerifyCodeService.class);
        sendResetPasswordSms = mock(SendResetPasswordSmsService.class);
        verifyResetPasswordCode = mock(VerifyResetPasswordCodeService.class);
        sendFindNicknameSms = mock(SendFindNicknameSmsService.class);
        verifyFindNicknameCode = mock(VerifyFindNicknameCodeService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SmsController(sendSms, verifyCode, sendResetPasswordSms,
                        verifyResetPasswordCode, sendFindNicknameSms, verifyFindNicknameCode))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("SMS 종류별 경로는")
    class RouteSelection {
        @Test
        void it_routes_signup_sms_to_send_service() throws Exception {
            mvc.perform(post("/api/sms").contentType(MediaType.APPLICATION_JSON).content(phoneRequest()))
                    .andExpect(status().isOk()).andExpect(content().string(""));
            verify(sendSms).execute(any());
        }

        @Test
        void it_routes_signup_verification_to_verify_service() throws Exception {
            mvc.perform(post("/api/sms/verify").contentType(MediaType.APPLICATION_JSON).content(codeRequest()))
                    .andExpect(status().isOk()).andExpect(content().string(""));
            verify(verifyCode).execute(any());
        }

        @Test
        void it_routes_password_sms_to_reset_service() throws Exception {
            mvc.perform(post("/api/sms/password").contentType(MediaType.APPLICATION_JSON).content(phoneRequest()))
                    .andExpect(status().isOk()).andExpect(content().string(""));
            verify(sendResetPasswordSms).execute(any());
        }

        @Test
        void it_routes_password_verification_to_reset_verify_service() throws Exception {
            mvc.perform(post("/api/sms/password/verify").contentType(MediaType.APPLICATION_JSON).content(codeRequest()))
                    .andExpect(status().isOk()).andExpect(content().string(""));
            verify(verifyResetPasswordCode).execute(any());
        }

        @Test
        void it_routes_nickname_sms_to_find_nickname_service() throws Exception {
            mvc.perform(post("/api/sms/nickname").contentType(MediaType.APPLICATION_JSON).content(phoneRequest()))
                    .andExpect(status().isOk()).andExpect(content().string(""));
            verify(sendFindNicknameSms).execute(any());
        }

        @Test
        void it_routes_nickname_verification_to_find_nickname_verify_service() throws Exception {
            mvc.perform(post("/api/sms/nickname/verify").contentType(MediaType.APPLICATION_JSON).content(codeRequest()))
                    .andExpect(status().isOk()).andExpect(content().string(""));
            verify(verifyFindNicknameCode).execute(any());
        }
    }

    @Nested
    @DisplayName("SMS 요청 오류는")
    class Errors {
        @Test
        void it_rejects_invalid_phone_before_service_call() throws Exception {
            mvc.perform(post("/api/sms").contentType(MediaType.APPLICATION_JSON).content("{\"phoneNumber\":\"123\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

            verifyNoInteractions(sendSms);
        }

        @Test
        void it_maps_domain_error_to_its_http_payload() throws Exception {
            doThrow(new AlreadyRegisteredPhoneNumberException()).when(sendSms).execute(any());

            mvc.perform(post("/api/sms").contentType(MediaType.APPLICATION_JSON).content("{\"phoneNumber\":\"01012345678\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("이미 가입된 전화번호입니다."));
        }
    }

    private String phoneRequest() {
        return "{\"phoneNumber\":\"01012345678\"}";
    }

    private String codeRequest() {
        return "{\"phoneNumber\":\"01012345678\",\"code\":\"123456\"}";
    }
}
