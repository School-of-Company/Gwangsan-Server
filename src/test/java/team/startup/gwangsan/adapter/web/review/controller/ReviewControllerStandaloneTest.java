package team.startup.gwangsan.adapter.web.review.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.adapter.web.support.FixedActorResolver;
import team.startup.gwangsan.application.review.data.*;
import team.startup.gwangsan.application.review.port.in.CreateReviewUseCase;
import team.startup.gwangsan.application.review.port.in.ReviewQuery;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("후기 API 독립 HTTP 계약")
class ReviewControllerStandaloneTest {
    private static final long ACTOR = 3L;

    private MockMvc mvc;
    private CreateReviewUseCase createReviewUseCase;
    private ReviewQuery reviewQuery;

    @BeforeEach
    void setUp() {
        createReviewUseCase = mock(CreateReviewUseCase.class);
        reviewQuery = mock(ReviewQuery.class);
        mvc = MockMvcBuilders.standaloneSetup(new ReviewController(createReviewUseCase, reviewQuery))
                .setCustomArgumentResolvers(FixedActorResolver.user(ACTOR))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("후기 생성은")
    class CreateReview {
        @Test
        void it_binds_valid_body_and_returns_created_without_body() throws Exception {
            mvc.perform(post("/api/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\":4,\"otherMemberId\":8,\"content\":\"친절했습니다\",\"light\":95}"))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));

            verify(createReviewUseCase).execute(new CreateReviewCommand(ACTOR, 4L, 8L, "친절했습니다", 95));
        }

        @Test
        void it_rejects_out_of_range_light_before_use_case() throws Exception {
            mvc.perform(post("/api/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\":4,\"otherMemberId\":8,\"content\":\"친절했습니다\",\"light\":101}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(createReviewUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("후기 목록 조회는")
    class FindReviews {
        @Test
        void it_serializes_my_reviews_from_base_route() throws Exception {
            when(reviewQuery.findWritten(ACTOR)).thenReturn(List.of(new ReviewItem(1L, 4L, "좋아요", 80, "작성자", "대상", List.of())));

            mvc.perform(get("/api/review"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].reviewId").value(1))
                    .andExpect(jsonPath("$[0].productId").value(4))
                    .andExpect(jsonPath("$[0].content").value("좋아요"))
                    .andExpect(jsonPath("$[0].light").value(80))
                    .andExpect(jsonPath("$[0].reviewerName").value("작성자"))
                    .andExpect(jsonPath("$[0].targetName").value("대상"))
                    .andExpect(jsonPath("$[0].imageUrls").isEmpty());
        }

        @Test
        void it_routes_current_before_member_path() throws Exception {
            when(reviewQuery.findReceived(ACTOR, ReviewPage.ALL)).thenReturn(List.of(new ReviewItem(2L, 5L, "고마워요", 90, "작성자", "나", List.of())));

            mvc.perform(get("/api/review/current"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].targetName").value("나"));

            verify(reviewQuery).findReceived(ACTOR, ReviewPage.ALL);
        }

        @Test
        void it_binds_cursor_and_size_for_current() throws Exception {
            when(reviewQuery.findReceived(ACTOR, new ReviewPage(10L, 2))).thenReturn(List.of());

            mvc.perform(get("/api/review/current").param("cursor", "10").param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        void it_binds_member_path_to_member_review_query() throws Exception {
            when(reviewQuery.findByMember(8L, ReviewPage.ALL)).thenReturn(List.of(new ReviewItem(3L, 6L, "후기", 70, "작성자", "대상",
                    List.of(new ReviewImage(1L, "cdn/1.png")))));

            mvc.perform(get("/api/review/8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].productId").value(6))
                    .andExpect(jsonPath("$[0].imageUrls[0].imageId").value(1))
                    .andExpect(jsonPath("$[0].imageUrls[0].imageUrl").value("cdn/1.png"));
        }
    }

    @Nested
    @DisplayName("후기 상세 조회는")
    class FindReviewDetail {
        @Test
        void it_serializes_detail_and_nested_images() throws Exception {
            when(reviewQuery.findDetail(4L)).thenReturn(new ReviewDetail(4L, 7L, "상품", "상세", 88,
                    List.of(new ReviewImage(2L, "cdn.example/2.png"))));

            mvc.perform(get("/api/review/detail/4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviewId").value(4))
                    .andExpect(jsonPath("$.productId").value(7))
                    .andExpect(jsonPath("$.title").value("상품"))
                    .andExpect(jsonPath("$.content").value("상세"))
                    .andExpect(jsonPath("$.light").value(88))
                    .andExpect(jsonPath("$.imageUrls[0].imageId").value(2));
        }

        @Test
        void it_converts_missing_detail_to_not_found_response() throws Exception {
            when(reviewQuery.findDetail(4L)).thenThrow(new NotFoundReviewException());

            mvc.perform(get("/api/review/detail/4"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
