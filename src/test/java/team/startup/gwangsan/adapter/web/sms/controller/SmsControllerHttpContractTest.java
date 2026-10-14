package team.startup.gwangsan.adapter.web.sms.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.domain.sms.exception.AlreadyRegisteredPhoneNumberException;
import team.startup.gwangsan.application.sms.port.in.SendFindNicknameSmsUseCase;
import team.startup.gwangsan.application.sms.port.in.SendResetPasswordSmsUseCase;
import team.startup.gwangsan.application.sms.port.in.SendSmsUseCase;
import team.startup.gwangsan.application.sms.port.in.VerifyCodeUseCase;
import team.startup.gwangsan.infrastructure.web.GlobalExceptionHandler;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SMS HTTP 계약")
class SmsControllerHttpContractTest {
    private MockMvc mvc;
    private SendSmsUseCase sendSms;
    private SendResetPasswordSmsUseCase sendResetPasswordSms;
    private SendFindNicknameSmsUseCase sendFindNicknameSms;
    private VerifyCodeUseCase verifyCode;

    @BeforeEach
    void setUp() {
        sendSms = mock(SendSmsUseCase.class);
        sendResetPasswordSms = mock(SendResetPasswordSmsUseCase.class);
        sendFindNicknameSms = mock(SendFindNicknameSmsUseCase.class);
        verifyCode = mock(VerifyCodeUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new SmsController(sendSms, sendResetPasswordSms, sendFindNicknameSms, verifyCode))
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
            verify(sendSms).execute("01012345678");
        }

        @Test
        void it_routes_signup_verification_to_verify_service() throws Exception {
            mvc.perform(post("/api/sms/verify").contentType(MediaType.APPLICATION_JSON).content(codeRequest()))
                    .andExpect(status().isOk()).andExpect(content().string(""));
            verify(verifyCode).execute("01012345678", "123456");
        }

        @Test
        void it_routes_password_sms_to_reset_service() throws Exception {
            mvc.perform(post("/api/sms/password").contentType(MediaType.APPLICATION_JSON).content(phoneRequest()))
                    .andExpect(status().isOk()).andExpect(content().string(""));
            verify(sendResetPasswordSms).execute("01012345678");
        }

        @Test
        void it_routes_password_verification_to_reset_verify_service() throws Exception {
            mvc.perform(post("/api/sms/password/verify").contentType(MediaType.APPLICATION_JSON).content(codeRequest()))
                    .andExpect(status().isOk()).andExpect(content().string(""));
            verify(verifyCode).execute("01012345678", "123456");
        }

        @Test
        void it_routes_nickname_sms_to_find_nickname_service() throws Exception {
            mvc.perform(post("/api/sms/nickname").contentType(MediaType.APPLICATION_JSON).content(phoneRequest()))
                    .andExpect(status().isOk()).andExpect(content().string(""));
            verify(sendFindNicknameSms).execute("01012345678");
        }

        @Test
        void it_routes_nickname_verification_to_find_nickname_verify_service() throws Exception {
            mvc.perform(post("/api/sms/nickname/verify").contentType(MediaType.APPLICATION_JSON).content(codeRequest()))
                    .andExpect(status().isOk()).andExpect(content().string(""));
            verify(verifyCode).execute("01012345678", "123456");
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

            verifyNoInteractions(sendSms, sendResetPasswordSms, sendFindNicknameSms, verifyCode);
        }

        @Test
        void it_maps_domain_error_to_its_http_payload() throws Exception {
            doThrow(new AlreadyRegisteredPhoneNumberException()).when(sendSms).execute(anyString());

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
