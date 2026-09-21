package team.startup.gwangsan.domain.review.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import team.startup.gwangsan.domain.review.service.*;
import team.startup.gwangsan.global.auth.MemberDetailsService;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;
import team.startup.gwangsan.global.security.config.SecurityConfig;
import team.startup.gwangsan.global.security.handler.JwtAccessDeniedHandler;
import team.startup.gwangsan.global.security.handler.JwtAuthenticationEntryPoint;
import team.startup.gwangsan.global.security.jwt.JwtProvider;
import team.startup.gwangsan.global.security.jwt.TokenParser;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@ContextConfiguration(classes = {ReviewController.class, SecurityConfig.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
@DisplayName("받은 후기 API 계약")
class ReviewControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CreateReviewService create;
    @MockitoBean GetMyReviewListService written;
    @MockitoBean GetReceivedReviewListService received;
    @MockitoBean GetReviewByMemberService byMember;
    @MockitoBean GetReviewDetailService detail;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean TokenParser tokenParser;
    @MockitoBean MemberDetailsService memberDetailsService;

    @Nested
    @DisplayName("인증 및 입력 경계")
    class Validation {
        @ParameterizedTest
        @ValueSource(strings = {"size=", "size=2&cursor=", "size=0", "size=-1", "size=101", "size=abc", "size=2147483648",
                "size=2&cursor=0", "size=2&cursor=-1", "size=2&cursor=abc",
                "size=2&cursor=9223372036854775808", "cursor=2"})
        void it_rejects_invalid_pagination(String query) throws Exception {
            for (String path : new String[]{"current", "7"}) {
                mvc.perform(get("/api/review/" + path + "?" + query).with(user("member")))
                        .andExpect(status().isBadRequest());
            }
            verifyNoInteractions(received, byMember);
        }

        @Test
        void it_requires_authentication_for_both_modes() throws Exception {
            for (String path : new String[]{"current", "7"}) {
                mvc.perform(get("/api/review/" + path)).andExpect(status().isUnauthorized());
                mvc.perform(get("/api/review/" + path + "?size=2")).andExpect(status().isUnauthorized());
            }
            verifyNoInteractions(received, byMember);
        }
    }
}
