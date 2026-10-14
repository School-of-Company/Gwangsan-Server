package team.startup.gwangsan.adapter.web.review.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import team.startup.gwangsan.adapter.security.auth.ActorContextArgumentResolver;
import team.startup.gwangsan.adapter.security.auth.MemberDetails;
import team.startup.gwangsan.adapter.security.auth.MemberDetailsService;
import team.startup.gwangsan.adapter.security.auth.TokenParser;
import team.startup.gwangsan.application.review.port.in.CreateReviewUseCase;
import team.startup.gwangsan.application.review.port.in.ReviewQuery;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;
import team.startup.gwangsan.adapter.security.auth.JwtProvider;
import team.startup.gwangsan.infrastructure.security.JwtAccessDeniedHandler;
import team.startup.gwangsan.infrastructure.security.JwtAuthenticationEntryPoint;
import team.startup.gwangsan.infrastructure.security.SecurityConfig;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@ContextConfiguration(classes = {ReviewController.class, SecurityConfig.class, JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class, GlobalExceptionHandler.class, ActorContextArgumentResolver.class,
        ReviewControllerTest.ActorResolverConfig.class})
@DisplayName("받은 후기 API 계약")
class ReviewControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CreateReviewUseCase createReviewUseCase;
    @MockitoBean ReviewQuery reviewQuery;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean TokenParser tokenParser;
    @MockitoBean MemberDetailsService memberDetailsService;

    /** 운영 WebMvcConfig 와 같이 ActorContext resolver 를 등록한다(CORS 설정 값 없이). */
    @TestConfiguration
    static class ActorResolverConfig implements WebMvcConfigurer {
        @Autowired ActorContextArgumentResolver resolver;

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(resolver);
        }
    }

    private static MemberDetails principal() {
        return new MemberDetails(new MemberAccount(1L, "member", "member", "hash", MemberRole.ROLE_USER, MemberStatus.ACTIVE));
    }

    @Nested
    @DisplayName("인증 및 입력 경계")
    class Validation {
        @ParameterizedTest
        @ValueSource(strings = {"size=", "size=2&cursor=", "size=0", "size=-1", "size=101", "size=abc", "size=2147483648",
                "size=2&cursor=0", "size=2&cursor=-1", "size=2&cursor=abc",
                "size=2&cursor=9223372036854775808", "cursor=2"})
        void it_rejects_invalid_pagination(String query) throws Exception {
            for (String path : new String[]{"current", "7"}) {
                mvc.perform(get("/api/review/" + path + "?" + query).with(user(principal())))
                        .andExpect(status().isBadRequest());
            }
            verifyNoInteractions(reviewQuery);
        }

        @Test
        void it_requires_authentication_for_both_modes() throws Exception {
            for (String path : new String[]{"current", "7"}) {
                mvc.perform(get("/api/review/" + path)).andExpect(status().isUnauthorized());
                mvc.perform(get("/api/review/" + path + "?size=2")).andExpect(status().isUnauthorized());
            }
            verifyNoInteractions(reviewQuery);
        }
    }
}
