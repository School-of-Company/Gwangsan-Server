package team.startup.gwangsan.adapter.web.feedback;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import team.startup.gwangsan.adapter.security.auth.*;
import team.startup.gwangsan.application.feedback.port.in.*;
import team.startup.gwangsan.application.feedback.data.FeedbackDetail;
import team.startup.gwangsan.domain.feedback.model.*;
import team.startup.gwangsan.domain.feedback.exception.*;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.domain.member.model.*;
import team.startup.gwangsan.infrastructure.web.GlobalExceptionHandler;
import team.startup.gwangsan.infrastructure.config.WebMvcConfig;
import team.startup.gwangsan.infrastructure.security.*;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({FeedbackController.class, AdminFeedbackController.class})
@ContextConfiguration(classes = {FeedbackController.class, AdminFeedbackController.class, SecurityConfig.class,
        WebMvcConfig.class, ActorContextArgumentResolver.class, JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost")
@DisplayName("문의 HTTP 계약과 인증")
class FeedbackControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CreateFeedbackUseCase createFeedback;
    @MockitoBean FeedbackQuery feedbackQuery;
    @MockitoBean UpdateFeedbackUseCase updateFeedback;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean TokenParser tokenParser;
    @MockitoBean MemberDetailsService memberDetailsService;

    private MemberDetails principal(MemberRole role) {
        return new MemberDetails(new MemberAccount(7L, "회원", "010", "pw", role, MemberStatus.ACTIVE));
    }

    @Nested
    @DisplayName("사용자 API는")
    class UserApi {
        @Test
        void it_creates_with_authenticated_member_and_empty_body() throws Exception {
            mvc.perform(post("/api/feedback").with(user(principal(MemberRole.ROLE_USER)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"memberId":999,"type":"BUG","content":"문의","imageIds":[1,2],"osType":"IOS"}
                            """)).andExpect(status().isCreated()).andExpect(content().string(""));
            verify(createFeedback).execute(argThat(c -> c.memberId().equals(7L) && c.imageIds().equals(List.of(1L, 2L))));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "{\"type\":\"UNKNOWN\",\"content\":\"문의\",\"imageIds\":[],\"osType\":\"IOS\"}",
                "{\"type\":\"BUG\",\"content\":\" \",\"imageIds\":[],\"osType\":\"IOS\"}",
                "{\"type\":\"BUG\",\"content\":\"문의\",\"imageIds\":[0],\"osType\":\"IOS\"}",
                "{\"type\":\"BUG\",\"content\":\"문의\",\"imageIds\":[null],\"osType\":\"IOS\"}",
                "{\"type\":\"BUG\",\"content\":\"문의\",\"imageIds\":[1,2,3,4,5,6],\"osType\":\"IOS\"}",
                "{\"type\":\"BUG\",\"content\":\"문의\",\"imageIds\":[],\"osType\":\"WEB\"}",
                "{\"type\":\"BUG\",\"content\":\"문의\",\"osType\":\"IOS\"}"})
        void it_returns_400_message_for_invalid_body(String body) throws Exception {
            mvc.perform(post("/api/feedback").with(user(principal(MemberRole.ROLE_USER)))
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
            verifyNoInteractions(createFeedback);
        }

        @Test
        void it_rejects_overlong_content_and_missing_image_as_400() throws Exception {
            mvc.perform(post("/api/feedback").with(user(principal(MemberRole.ROLE_USER)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"BUG\",\"content\":\"" + "가".repeat(1001) + "\",\"imageIds\":[],\"osType\":\"IOS\"}"))
                    .andExpect(status().isBadRequest());
            doThrow(new InvalidFeedbackException()).when(createFeedback).execute(any());
            mvc.perform(post("/api/feedback").with(user(principal(MemberRole.ROLE_USER)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"BUG\",\"content\":\"문의\",\"imageIds\":[999],\"osType\":\"IOS\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        void it_returns_only_user_contract_and_binds_empty_cursor() throws Exception {
            when(feedbackQuery.mine(7L, null, 10)).thenReturn(List.of(detail()));
            mvc.perform(get("/api/feedback/me?last_id=").with(user(principal(MemberRole.ROLE_USER))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].feedbackId").value(1))
                    .andExpect(jsonPath("$[0].images[0].imageId").value(5))
                    .andExpect(jsonPath("$[0].memberId").doesNotExist())
                    .andExpect(jsonPath("$[0].deviceModel").doesNotExist());
        }

        @Test
        void it_requires_authentication() throws Exception {
            mvc.perform(get("/api/feedback/me")).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/feedback").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("관리자 API는")
    class AdminApi {
        @Test
        void it_denies_user_for_every_admin_endpoint() throws Exception {
            var member = principal(MemberRole.ROLE_USER);
            mvc.perform(get("/api/admin/feedback").with(user(member))).andExpect(status().isForbidden());
            mvc.perform(get("/api/admin/feedback/1").with(user(member))).andExpect(status().isForbidden());
            mvc.perform(patch("/api/admin/feedback/1/answer").with(user(member))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"답변\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(patch("/api/admin/feedback/1/status").with(user(member))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(feedbackQuery, updateFeedback);
        }

        @Test
        void it_binds_filters_and_returns_admin_detail() throws Exception {
            when(feedbackQuery.list(any(), eq(FeedbackStatus.DONE), eq(FeedbackType.BUG), eq(3), eq(9L), eq(20)))
                    .thenReturn(List.of(detail()));
            mvc.perform(get("/api/admin/feedback?status=DONE&type=BUG&place_id=3&last_id=9")
                    .with(user(principal(MemberRole.ROLE_HEAD_ADMIN))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].memberId").value(7))
                    .andExpect(jsonPath("$[0].nickname").value("작성자"))
                    .andExpect(jsonPath("$[0].placeName").value("지점"))
                    .andExpect(jsonPath("$[0].osType").value("IOS"));
            when(feedbackQuery.detail(any(), eq(1L))).thenReturn(detail());
            mvc.perform(get("/api/admin/feedback/1").with(user(principal(MemberRole.ROLE_PLACE_ADMIN))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.feedbackId").value(1));
        }

        @Test
        void it_updates_answer_and_status() throws Exception {
            var admin = principal(MemberRole.ROLE_PLACE_ADMIN);
            mvc.perform(patch("/api/admin/feedback/1/answer").with(user(admin))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"답변\"}"))
                    .andExpect(status().isNoContent());
            mvc.perform(patch("/api/admin/feedback/1/status").with(user(admin))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
                    .andExpect(status().isNoContent());
            verify(updateFeedback).answer(argThat(a -> a.memberId().equals(7L)), eq(1L), eq("답변"));
            verify(updateFeedback).changeStatus(any(), eq(1L), eq(FeedbackStatus.IN_PROGRESS));
        }

        @Test
        void it_rejects_blank_answers_and_maps_not_found_and_scope_errors() throws Exception {
            var admin = principal(MemberRole.ROLE_HEAD_ADMIN);
            mvc.perform(patch("/api/admin/feedback/1/answer").with(user(admin))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\" \"}"))
                    .andExpect(status().isBadRequest());
            when(feedbackQuery.detail(any(), eq(1L))).thenThrow(new NotFoundFeedbackException());
            mvc.perform(get("/api/admin/feedback/1").with(user(admin))).andExpect(status().isNotFound());
            when(feedbackQuery.detail(any(), eq(1L))).thenThrow(new ForbiddenFeedbackException());
            mvc.perform(get("/api/admin/feedback/1").with(user(admin))).andExpect(status().isForbidden());
        }
    }

    private FeedbackDetail detail() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 22, 12, 0);
        return new FeedbackDetail(new Feedback(1L, 7L, FeedbackType.BUG, "문의", "1.2.2", OsType.IOS, "18",
                "iPhone", FeedbackStatus.RECEIVED, null, null, null, now, now), "작성자", 3, "지점", null,
                List.of(new FeedbackDetail.FeedbackImage(5L, "https://image")));
    }
}
