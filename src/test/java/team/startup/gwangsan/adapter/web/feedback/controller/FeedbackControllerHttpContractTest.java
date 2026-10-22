package team.startup.gwangsan.adapter.web.feedback.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.application.feedback.data.FeedbackImageLink;
import team.startup.gwangsan.application.feedback.data.FeedbackSummary;
import team.startup.gwangsan.application.feedback.data.MyFeedbackSearch;
import team.startup.gwangsan.application.feedback.port.in.CreateFeedbackUseCase;
import team.startup.gwangsan.application.feedback.port.in.FindMyFeedbacksQuery;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;
import team.startup.gwangsan.infrastructure.web.GlobalExceptionHandler;
import team.startup.gwangsan.support.FixedActorResolver;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("문의 HTTP 계약")
class FeedbackControllerHttpContractTest {

    private static final ActorContext ACTOR = new ActorContext(1L, ActorContext.Role.ROLE_USER);

    private MockMvc mvc;
    private CreateFeedbackUseCase createFeedbackUseCase;
    private FindMyFeedbacksQuery findMyFeedbacksQuery;

    @BeforeEach
    void setUp() {
        createFeedbackUseCase = mock(CreateFeedbackUseCase.class);
        findMyFeedbacksQuery = mock(FindMyFeedbacksQuery.class);
        mvc = MockMvcBuilders.standaloneSetup(new FeedbackController(createFeedbackUseCase, findMyFeedbacksQuery))
                .setCustomArgumentResolvers(new FixedActorResolver(ACTOR))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/feedback 는")
    class Describe_create {

        @Test
        @DisplayName("it_유효한 요청이면 201 을 반환한다")
        void it_creates_and_returns_201() throws Exception {
            mvc.perform(post("/api/feedback").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type":"BUG","content":"문의 내용","imageIds":[1,2],
                                     "appVersion":"1.2.2","osType":"IOS","osVersion":"18.0","deviceModel":"iPhone 15"}
                                    """))
                    .andExpect(status().isCreated());

            verify(createFeedbackUseCase).execute(any());
        }

        @Test
        @DisplayName("it_내용이 비어 있으면 400 을 반환한다")
        void it_rejects_blank_content() throws Exception {
            mvc.perform(post("/api/feedback").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"BUG\",\"content\":\"\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(createFeedbackUseCase);
        }
    }

    @Nested
    @DisplayName("GET /api/feedback/me 는")
    class Describe_find_my_feedbacks {

        @Test
        @DisplayName("it_내 문의 내역을 배열로 반환한다")
        void it_returns_my_feedbacks() throws Exception {
            LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
            when(findMyFeedbacksQuery.query(new MyFeedbackSearch(ACTOR, null, 10))).thenReturn(List.of(
                    new FeedbackSummary(1L, FeedbackType.BUG, "내용", List.of(new FeedbackImageLink(2L, "url")),
                            FeedbackStatus.RECEIVED, null, null, createdAt)));

            mvc.perform(get("/api/feedback/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].feedbackId").value(1))
                    .andExpect(jsonPath("$[0].type").value("BUG"))
                    .andExpect(jsonPath("$[0].images[0].imageId").value(2))
                    .andExpect(jsonPath("$[0].status").value("RECEIVED"));
        }

        @Test
        @DisplayName("it_size 가 1 미만이면 400 을 반환한다")
        void it_rejects_invalid_size() throws Exception {
            mvc.perform(get("/api/feedback/me").param("size", "0"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(findMyFeedbacksQuery);
        }
    }
}
