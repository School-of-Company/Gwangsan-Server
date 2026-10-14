package team.startup.gwangsan.adapter.web.auth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.domain.auth.exception.UnauthorizedException;
import team.startup.gwangsan.application.auth.data.CurrentMemberResult;
import team.startup.gwangsan.application.auth.data.ResetPasswordCommand;
import team.startup.gwangsan.application.auth.data.SignInCommand;
import team.startup.gwangsan.application.auth.data.SignUpCommand;
import team.startup.gwangsan.application.auth.data.TokenResult;
import team.startup.gwangsan.application.auth.port.in.*;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;
import team.startup.gwangsan.support.FixedActorResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("인증 HTTP 계약")
class AuthControllerHttpContractTest {
    private static final ActorContext ACTOR = new ActorContext(7L, ActorContext.Role.ROLE_USER);

    private MockMvc mvc;
    private SignUpUseCase signUpUseCase;
    private SignInUseCase signInUseCase;
    private ReissueTokenUseCase reissueTokenUseCase;
    private SignOutUseCase signOutUseCase;
    private CurrentMemberQuery currentMemberQuery;
    private ResetPasswordUseCase resetPasswordUseCase;
    private FindNicknameQuery findNicknameQuery;

    @BeforeEach
    void setUp() {
        signUpUseCase = mock(SignUpUseCase.class);
        signInUseCase = mock(SignInUseCase.class);
        reissueTokenUseCase = mock(ReissueTokenUseCase.class);
        signOutUseCase = mock(SignOutUseCase.class);
        currentMemberQuery = mock(CurrentMemberQuery.class);
        resetPasswordUseCase = mock(ResetPasswordUseCase.class);
        findNicknameQuery = mock(FindNicknameQuery.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(
                        signUpUseCase, signInUseCase, reissueTokenUseCase, signOutUseCase,
                        currentMemberQuery, resetPasswordUseCase, findNicknameQuery))
                .setCustomArgumentResolvers(new FixedActorResolver(ACTOR))
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

            ArgumentCaptor<SignUpCommand> command = ArgumentCaptor.forClass(SignUpCommand.class);
            verify(signUpUseCase).execute(command.capture());
            assertThat(command.getValue()).isEqualTo(new SignUpCommand("홍길동", "길동", "password", "01012345678",
                    "수완동", 1, List.of("요리"), "추천인", "소개"));
        }

        @Test
        void it_rejects_invalid_signup_before_calling_service() throws Exception {
            mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\",\"placeId\":null,\"specialties\":[]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

            verifyNoInteractions(signUpUseCase);
        }

        @Test
        void it_serializes_tokens_and_maps_domain_error() throws Exception {
            when(signInUseCase.execute(any())).thenReturn(new TokenResult("access", "refresh", null, null));

            mvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON).content("""
                    {"nickname":"길동","password":"password","deviceToken":"device","deviceId":"id","osType":"ANDROID"}
                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh"));

            verify(signInUseCase).execute(new SignInCommand("길동", "password", "device", "id", "ANDROID"));
            when(signInUseCase.execute(any())).thenThrow(new UnauthorizedException());
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
            when(currentMemberQuery.query(ACTOR)).thenReturn(new CurrentMemberResult(7L, "길동"));

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

            verify(resetPasswordUseCase).execute(new ResetPasswordCommand("01012345678", "new-password"));
        }

        @Test
        void it_serializes_found_nickname() throws Exception {
            when(findNicknameQuery.query("01012345678")).thenReturn("찾은별칭");

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
            when(reissueTokenUseCase.execute("refresh-token"))
                    .thenReturn(new TokenResult("new-access", "new-refresh", null, null));

            mvc.perform(patch("/api/auth/reissue").header("RefreshToken", "refresh-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access"));
            mvc.perform(delete("/api/auth/signout").header("Authorization", "Bearer access-token"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(reissueTokenUseCase).execute("refresh-token");
            verify(signOutUseCase).execute(ACTOR, "access-token");
        }
    }
}
