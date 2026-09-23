package team.startup.gwangsan.domain.auth.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.domain.auth.presentation.dto.request.SignUpRequest;
import team.startup.gwangsan.domain.auth.presentation.dto.response.FindNicknameResponse;
import team.startup.gwangsan.domain.auth.presentation.dto.response.MemberInfoResponse;
import team.startup.gwangsan.domain.auth.presentation.dto.response.TokenResponse;
import team.startup.gwangsan.domain.auth.service.FindNicknameService;
import team.startup.gwangsan.domain.auth.service.ReissueTokenService;
import team.startup.gwangsan.domain.auth.service.ResetPasswordService;
import team.startup.gwangsan.domain.auth.service.SignInService;
import team.startup.gwangsan.domain.auth.service.SignOutService;
import team.startup.gwangsan.domain.auth.service.SignUpService;
import team.startup.gwangsan.domain.auth.service.TokenAuthenticationService;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("인증 HTTP 계약")
class AuthControllerHttpContractTest {
    private MockMvc mvc;
    private SignUpService signUpService;
    private SignInService signInService;
    private ReissueTokenService reissueTokenService;
    private SignOutService signOutService;
    private TokenAuthenticationService tokenAuthenticationService;
    private ResetPasswordService resetPasswordService;
    private FindNicknameService findNicknameService;

    @BeforeEach
    void setUp() {
        signUpService = mock(SignUpService.class);
        signInService = mock(SignInService.class);
        reissueTokenService = mock(ReissueTokenService.class);
        signOutService = mock(SignOutService.class);
        tokenAuthenticationService = mock(TokenAuthenticationService.class);
        resetPasswordService = mock(ResetPasswordService.class);
        findNicknameService = mock(FindNicknameService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(
                        signUpService, signInService, reissueTokenService, signOutService,
                        tokenAuthenticationService, resetPasswordService, findNicknameService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("가입과 로그인 요청은")
    class RequestBinding {
        @Test
        void it_binds_valid_signup_and_returns_created_without_body() throws Exception {
            mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"홍길동","nickname":"길동","password":"password","phoneNumber":"01012345678",
                    "dongName":"수완동","placeId":1,"specialties":["요리"],"recommender":"추천인","description":"소개"}
                    """))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));

            ArgumentCaptor<SignUpRequest> request = ArgumentCaptor.forClass(SignUpRequest.class);
            verify(signUpService).execute(request.capture());
            assertThat(request.getValue().specialties()).isEqualTo(List.of("요리"));
        }

        @Test
        void it_rejects_invalid_signup_before_calling_service() throws Exception {
            mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\",\"placeId\":null,\"specialties\":[]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

            verifyNoInteractions(signUpService);
        }

        @Test
        void it_serializes_tokens_and_maps_domain_error() throws Exception {
            when(signInService.execute(any())).thenReturn(new TokenResponse("access", "refresh", null, null));

            mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON).content("""
                    {"nickname":"길동","password":"password","deviceToken":"device","deviceId":"id","osType":"ANDROID"}
                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh"));

            when(signInService.execute(any())).thenThrow(new UnauthorizedException());
            mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON).content("""
                    {"nickname":"길동","password":"password"}
                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("닉네임 또는 비밀번호가 잘못되었습니다."));
        }
    }

    @Nested
    @DisplayName("회원 조회와 복구 요청은")
    class AccountResponses {
        @Test
        void it_serializes_current_member_info() throws Exception {
            when(tokenAuthenticationService.execute()).thenReturn(new MemberInfoResponse(7L, "길동"));

            mvc.perform(get("/api/auth"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberId").value(7))
                    .andExpect(jsonPath("$.nickname").value("길동"));
        }

        @Test
        void it_binds_password_reset_and_returns_empty_ok_response() throws Exception {
            mvc.perform(patch("/api/auth/password").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phoneNumber\":\"01012345678\",\"newPassword\":\"new-password\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            verify(resetPasswordService).execute(any());
        }

        @Test
        void it_serializes_found_nickname() throws Exception {
            when(findNicknameService.execute(any())).thenReturn(new FindNicknameResponse("찾은별칭"));

            mvc.perform(post("/api/auth/nickname").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phoneNumber\":\"01012345678\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("찾은별칭"));
        }
    }

    @Nested
    @DisplayName("토큰 헤더는")
    class TokenHeaders {
        @Test
        void it_extracts_refresh_and_bearer_tokens_for_their_routes() throws Exception {
            when(reissueTokenService.execute("refresh-token"))
                    .thenReturn(new TokenResponse("new-access", "new-refresh", null, null));

            mvc.perform(patch("/api/auth/reissue").header("RefreshToken", "refresh-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access"));
            mvc.perform(delete("/api/auth/signout").header("Authorization", "Bearer access-token"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(reissueTokenService).execute("refresh-token");
            verify(signOutService).execute("access-token");
        }
    }
}
