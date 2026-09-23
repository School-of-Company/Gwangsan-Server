package team.startup.gwangsan.domain.review.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;
import team.startup.gwangsan.domain.review.presentation.dto.request.CreateReviewRequest;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewDetailResponse;
import team.startup.gwangsan.domain.review.presentation.dto.response.ReviewResponse;
import team.startup.gwangsan.domain.review.service.CreateReviewService;
import team.startup.gwangsan.domain.review.service.GetMyReviewListService;
import team.startup.gwangsan.domain.review.service.GetReceivedReviewListService;
import team.startup.gwangsan.domain.review.service.GetReviewByMemberService;
import team.startup.gwangsan.domain.review.service.GetReviewDetailService;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("후기 API 독립 HTTP 계약")
class ReviewControllerStandaloneTest {
    private MockMvc mvc;
    private CreateReviewService createReviewService;
    private GetMyReviewListService getMyReviewListService;
    private GetReceivedReviewListService getReceivedReviewListService;
    private GetReviewByMemberService getReviewByMemberService;
    private GetReviewDetailService getReviewDetailService;

    @BeforeEach
    void setUp() {
        createReviewService = mock(CreateReviewService.class);
        getMyReviewListService = mock(GetMyReviewListService.class);
        getReceivedReviewListService = mock(GetReceivedReviewListService.class);
        getReviewByMemberService = mock(GetReviewByMemberService.class);
        getReviewDetailService = mock(GetReviewDetailService.class);
        ReviewController controller = new ReviewController(
                createReviewService,
                getMyReviewListService,
                getReceivedReviewListService,
                getReviewByMemberService,
                getReviewDetailService
        );
        mvc = MockMvcBuilders.standaloneSetup(controller)
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

            verify(createReviewService).execute(new CreateReviewRequest(4L, 8L, "친절했습니다", 95));
        }

        @Test
        void it_rejects_out_of_range_light_before_service_execution() throws Exception {
            mvc.perform(post("/api/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\":4,\"otherMemberId\":8,\"content\":\"친절했습니다\",\"light\":101}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(createReviewService, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("후기 목록 조회는")
    class FindReviews {
        @Test
        void it_serializes_my_reviews_from_base_route() throws Exception {
            when(getMyReviewListService.execute()).thenReturn(List.of(new ReviewResponse(
                    1L, 4L, "좋아요", 80, "작성자", "대상", List.of()
            )));

            mvc.perform(get("/api/review"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].reviewId").value(1))
                    .andExpect(jsonPath("$[0].reviewerName").value("작성자"))
                    .andExpect(jsonPath("$[0].imageUrls").isEmpty());

            verify(getMyReviewListService).execute();
        }

        @Test
        void it_routes_current_before_member_path() throws Exception {
            when(getReceivedReviewListService.execute()).thenReturn(List.of(new ReviewResponse(
                    2L, 5L, "고마워요", 90, "작성자", "나", List.of()
            )));

            mvc.perform(get("/api/review/current"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].targetName").value("나"));

            verify(getReceivedReviewListService).execute();
        }

        @Test
        void it_binds_member_path_to_member_review_service() throws Exception {
            when(getReviewByMemberService.execute(8L)).thenReturn(List.of(new ReviewResponse(
                    3L, 6L, "후기", 70, "작성자", "대상", List.of()
            )));

            mvc.perform(get("/api/review/8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].productId").value(6));

            verify(getReviewByMemberService).execute(8L);
        }
    }

    @Nested
    @DisplayName("후기 상세 조회는")
    class FindReviewDetail {
        @Test
        void it_serializes_detail_and_nested_images() throws Exception {
            when(getReviewDetailService.execute(4L)).thenReturn(new ReviewDetailResponse(
                    4L, 7L, "상품", "상세", 88, List.of(new GetImageResponse(2L, "cdn.example/2.png"))
            ));

            mvc.perform(get("/api/review/detail/4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("상품"))
                    .andExpect(jsonPath("$.imageUrls[0].imageId").value(2));
        }

        @Test
        void it_converts_missing_detail_to_not_found_response() throws Exception {
            when(getReviewDetailService.execute(4L)).thenThrow(new NotFoundReviewException());

            mvc.perform(get("/api/review/detail/4"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
