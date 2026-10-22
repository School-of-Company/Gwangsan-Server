package team.startup.gwangsan.adapter.web.admin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.application.feedback.data.AdminFeedbackSearch;
import team.startup.gwangsan.application.feedback.data.AnswerFeedbackCommand;
import team.startup.gwangsan.application.feedback.data.ChangeFeedbackStatusCommand;
import team.startup.gwangsan.application.feedback.data.FeedbackDetail;
import team.startup.gwangsan.application.feedback.port.in.AnswerFeedbackUseCase;
import team.startup.gwangsan.application.feedback.port.in.ChangeFeedbackStatusUseCase;
import team.startup.gwangsan.application.feedback.port.in.FindAdminFeedbackQuery;
import team.startup.gwangsan.application.feedback.port.in.FindAdminFeedbacksQuery;
import team.startup.gwangsan.domain.admin.exception.AdminPlaceMismatchException;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;
import team.startup.gwangsan.infrastructure.web.GlobalExceptionHandler;
import team.startup.gwangsan.support.FixedActorResolver;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("관리자 문의 HTTP 계약")
class AdminFeedbackControllerHttpContractTest {

    private static final ActorContext HEAD_ADMIN = new ActorContext(1L, ActorContext.Role.ROLE_HEAD_ADMIN);

    private MockMvc mvc;
    private FindAdminFeedbacksQuery findAdminFeedbacksQuery;
    private FindAdminFeedbackQuery findAdminFeedbackQuery;
    private AnswerFeedbackUseCase answerFeedbackUseCase;
    private ChangeFeedbackStatusUseCase changeFeedbackStatusUseCase;

    @BeforeEach
    void setUp() {
        findAdminFeedbacksQuery = mock(FindAdminFeedbacksQuery.class);
        findAdminFeedbackQuery = mock(FindAdminFeedbackQuery.class);
        answerFeedbackUseCase = mock(AnswerFeedbackUseCase.class);
        changeFeedbackStatusUseCase = mock(ChangeFeedbackStatusUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new AdminFeedbackController(findAdminFeedbacksQuery,
                        findAdminFeedbackQuery, answerFeedbackUseCase, changeFeedbackStatusUseCase))
                .setCustomArgumentResolvers(new FixedActorResolver(HEAD_ADMIN))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static FeedbackDetail detail() {
        return new FeedbackDetail(1L, 9L, "닉네임", 4, "신창", FeedbackType.BUG, "내용", List.of(), null, null, null, null,
                FeedbackStatus.RECEIVED, null, null, null, null);
    }

    @Nested
    @DisplayName("GET /api/admin/feedback 는")
    class Describe_list {

        @Test
        @DisplayName("it_목록을 반환한다")
        void it_returns_list() throws Exception {
            when(findAdminFeedbacksQuery.query(new AdminFeedbackSearch(HEAD_ADMIN, null, null, null, null, 20)))
                    .thenReturn(List.of(detail()));

            mvc.perform(get("/api/admin/feedback"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].feedbackId").value(1))
                    .andExpect(jsonPath("$[0].nickname").value("닉네임"))
                    .andExpect(jsonPath("$[0].placeName").value("신창"));
        }

        @Test
        @DisplayName("it_size 가 1 미만이면 400 을 반환한다")
        void it_rejects_invalid_size() throws Exception {
            mvc.perform(get("/api/admin/feedback").param("size", "0"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(findAdminFeedbacksQuery);
        }
    }

    @Nested
    @DisplayName("GET /api/admin/feedback/{feedback_id} 는")
    class Describe_detail {

        @Test
        @DisplayName("it_범위 밖이면 기존 403 오류 본문으로 응답한다")
        void it_maps_place_mismatch() throws Exception {
            when(findAdminFeedbackQuery.query(HEAD_ADMIN, 1L)).thenThrow(new AdminPlaceMismatchException());

            mvc.perform(get("/api/admin/feedback/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/feedback/{feedback_id}/answer 는")
    class Describe_answer {

        @Test
        @DisplayName("it_답변을 저장하고 200 을 반환한다")
        void it_answers() throws Exception {
            mvc.perform(patch("/api/admin/feedback/1/answer").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"answer\":\"답변 내용\"}"))
                    .andExpect(status().isOk());

            verify(answerFeedbackUseCase).execute(new AnswerFeedbackCommand(HEAD_ADMIN, 1L, "답변 내용"));
        }

        @Test
        @DisplayName("it_답변이 비어 있으면 400 을 반환한다")
        void it_rejects_blank_answer() throws Exception {
            mvc.perform(patch("/api/admin/feedback/1/answer").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"answer\":\"\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(answerFeedbackUseCase);
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/feedback/{feedback_id}/status 는")
    class Describe_change_status {

        @Test
        @DisplayName("it_상태를 바꾸고 200 을 반환한다")
        void it_changes_status() throws Exception {
            mvc.perform(patch("/api/admin/feedback/1/status").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"IN_PROGRESS\"}"))
                    .andExpect(status().isOk());

            verify(changeFeedbackStatusUseCase)
                    .execute(new ChangeFeedbackStatusCommand(HEAD_ADMIN, 1L, FeedbackStatus.IN_PROGRESS));
        }
    }
}
