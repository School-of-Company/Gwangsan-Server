package team.startup.gwangsan.domain.post.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.domain.post.exception.InappropriateContentException;
import team.startup.gwangsan.domain.post.service.CreateProductService;
import team.startup.gwangsan.domain.post.service.DeleteProductByIdService;
import team.startup.gwangsan.domain.post.service.DeleteReservationProductService;
import team.startup.gwangsan.domain.post.service.FindProductByCurrentUserAndTypeAndModeService;
import team.startup.gwangsan.domain.post.service.FindProductByIdService;
import team.startup.gwangsan.domain.post.service.FindProductsByMemberIdService;
import team.startup.gwangsan.domain.post.service.FindProductsByTypeAndModeService;
import team.startup.gwangsan.domain.post.service.ProductContentValidationService;
import team.startup.gwangsan.domain.post.service.RequestTradeCompleteService;
import team.startup.gwangsan.domain.post.service.ReservationProductService;
import team.startup.gwangsan.domain.post.service.UpdateProductService;
import team.startup.gwangsan.domain.post.service.WithdrawTradeCompleteService;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("게시글 HTTP 계약")
class PostControllerTest {

    private MockMvc mvc;
    private CreateProductService createProductService;
    private ProductContentValidationService contentValidationService;
    private FindProductByIdService findProductByIdService;
    private FindProductsByTypeAndModeService findProductsByTypeAndModeService;
    private FindProductByCurrentUserAndTypeAndModeService findCurrentProductService;
    private UpdateProductService updateProductService;
    private DeleteProductByIdService deleteProductByIdService;
    private FindProductsByMemberIdService findProductsByMemberIdService;
    private RequestTradeCompleteService requestTradeCompleteService;
    private WithdrawTradeCompleteService withdrawTradeCompleteService;
    private ReservationProductService reservationProductService;
    private DeleteReservationProductService deleteReservationProductService;

    @BeforeEach
    void setUp() {
        createProductService = mock(CreateProductService.class);
        contentValidationService = mock(ProductContentValidationService.class);
        findProductByIdService = mock(FindProductByIdService.class);
        findProductsByTypeAndModeService = mock(FindProductsByTypeAndModeService.class);
        findCurrentProductService = mock(FindProductByCurrentUserAndTypeAndModeService.class);
        updateProductService = mock(UpdateProductService.class);
        deleteProductByIdService = mock(DeleteProductByIdService.class);
        findProductsByMemberIdService = mock(FindProductsByMemberIdService.class);
        requestTradeCompleteService = mock(RequestTradeCompleteService.class);
        withdrawTradeCompleteService = mock(WithdrawTradeCompleteService.class);
        reservationProductService = mock(ReservationProductService.class);
        deleteReservationProductService = mock(DeleteReservationProductService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PostController(
                        createProductService,
                        contentValidationService,
                        findProductByIdService,
                        findProductsByTypeAndModeService,
                        findCurrentProductService,
                        updateProductService,
                        deleteProductByIdService,
                        findProductsByMemberIdService,
                        requestTradeCompleteService,
                        withdrawTradeCompleteService,
                        reservationProductService,
                        deleteReservationProductService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("게시글 생성은")
    class Describe_create_product {

        @Test
        void it_moderates_bound_request_before_creating_product() throws Exception {
            mvc.perform(post("/api/post")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type":"OBJECT","mode":"GIVER","title":"자전거 수리","content":"브레이크를 고쳐 드립니다.","gwangsan":5000,"imageIds":[11,12]}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));

            InOrder order = inOrder(contentValidationService, createProductService);
            order.verify(contentValidationService).validate("자전거 수리", "브레이크를 고쳐 드립니다.");
            order.verify(createProductService).execute(
                    team.startup.gwangsan.domain.post.entity.constant.Type.OBJECT,
                    team.startup.gwangsan.domain.post.entity.constant.Mode.GIVER,
                    "자전거 수리", "브레이크를 고쳐 드립니다.", 5000, java.util.List.of(11L, 12L));
        }

        @Test
        void it_rejects_bean_validation_failure_without_moderating_or_creating() throws Exception {
            mvc.perform(post("/api/post")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type":"OBJECT","mode":"GIVER","title":"123456789012345678901","content":"설명","gwangsan":5000}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

            verifyNoInteractions(contentValidationService, createProductService);
        }

        @Test
        void it_maps_moderation_rejection_and_never_creates_product() throws Exception {
            doThrow(new InappropriateContentException()).when(contentValidationService)
                    .validate("금지 제목", "금지 내용");

            mvc.perform(post("/api/post")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type":"SERVICE","mode":"RECEIVER","title":"금지 제목","content":"금지 내용","gwangsan":5000}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("부적절한 내용이 포함되어 있습니다."));

            verify(contentValidationService).validate("금지 제목", "금지 내용");
            verifyNoInteractions(createProductService);
        }

        @Test
        void it_maps_unknown_enum_json_to_bad_request_without_services() throws Exception {
            mvc.perform(post("/api/post")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type":"UNKNOWN","mode":"GIVER","title":"제목","content":"설명","gwangsan":5000}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

            verifyNoInteractions(contentValidationService, createProductService);
        }
    }

    @Nested
    @DisplayName("게시글 수정은")
    class Describe_update_product {

        @Test
        void it_moderates_before_updating_and_returns_no_content() throws Exception {
            mvc.perform(patch("/api/post/7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type":"SERVICE","mode":"RECEIVER","title":"이사 도움","content":"짐을 옮겨 드립니다.","gwangsan":10000,"imageIds":[4]}
                                    """))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            InOrder order = inOrder(contentValidationService, updateProductService);
            order.verify(contentValidationService).validate("이사 도움", "짐을 옮겨 드립니다.");
            order.verify(updateProductService).execute(
                    7L, team.startup.gwangsan.domain.post.entity.constant.Type.SERVICE,
                    team.startup.gwangsan.domain.post.entity.constant.Mode.RECEIVER,
                    "이사 도움", "짐을 옮겨 드립니다.", 10000, List.of(4L));
        }

        @Test
        void it_maps_moderation_rejection_without_updating() throws Exception {
            doThrow(new InappropriateContentException()).when(contentValidationService)
                    .validate("금지 제목", "금지 내용");

            mvc.perform(patch("/api/post/7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type":"SERVICE","mode":"RECEIVER","title":"금지 제목","content":"금지 내용","gwangsan":10000}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("부적절한 내용이 포함되어 있습니다."));

            verifyNoInteractions(updateProductService);
        }
    }

    @Nested
    @DisplayName("게시글 조회는")
    class Describe_find_products {

        @Test
        void it_returns_product_for_bound_id() throws Exception {
            when(findProductByIdService.execute(7L)).thenReturn(
                    new team.startup.gwangsan.domain.post.presentation.dto.response.GetProductByIdResponse(
                            7L, "수리", "설명", 5000,
                            team.startup.gwangsan.domain.post.entity.constant.Type.OBJECT,
                            team.startup.gwangsan.domain.post.entity.constant.Mode.GIVER,
                            null, List.of(), true, false, false, false));
            mvc.perform(get("/api/post/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(7))
                    .andExpect(jsonPath("$.title").value("수리"));

            verify(findProductByIdService).execute(7L);
        }

        @Test
        void it_binds_explicit_listing_filters() throws Exception {
            when(findProductsByTypeAndModeService.execute(
                    team.startup.gwangsan.domain.post.entity.constant.Type.OBJECT,
                    team.startup.gwangsan.domain.post.entity.constant.Mode.GIVER)).thenReturn(List.of());

            mvc.perform(get("/api/post").param("type", "OBJECT").param("mode", "GIVER"))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));

            verify(findProductsByTypeAndModeService).execute(
                    team.startup.gwangsan.domain.post.entity.constant.Type.OBJECT,
                    team.startup.gwangsan.domain.post.entity.constant.Mode.GIVER);
        }

        @Test
        void it_binds_omitted_current_listing_filters_as_null() throws Exception {
            when(findCurrentProductService.execute(null, null)).thenReturn(List.of());

            mvc.perform(get("/api/post/current"))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));

            verify(findCurrentProductService).execute(null, null);
        }

        @Test
        void it_binds_member_listing_with_omitted_mode_as_null() throws Exception {
            when(findProductsByMemberIdService.execute(8L,
                    team.startup.gwangsan.domain.post.entity.constant.Type.SERVICE, null)).thenReturn(List.of());

            mvc.perform(get("/api/post/member/8").param("type", "SERVICE"))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));

            verify(findProductsByMemberIdService).execute(
                    8L, team.startup.gwangsan.domain.post.entity.constant.Type.SERVICE, null);
        }
    }

    @Nested
    @DisplayName("거래와 예약 요청은")
    class Describe_trade_and_reservation {

        @Test
        void it_binds_trade_request_body_and_returns_no_content() throws Exception {
            String request = "{\"productId\":7,\"otherMemberId\":8}";

            mvc.perform(post("/api/post/trade").contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));

            verify(requestTradeCompleteService).execute(7L, 8L);
        }

        @Test
        void it_binds_trade_withdraw_body_and_returns_no_content() throws Exception {
            String request = "{\"productId\":7,\"otherMemberId\":8}";

            mvc.perform(delete("/api/post/trade").contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));

            verify(withdrawTradeCompleteService).execute(7L, 8L);
        }

        @Test
        void it_binds_reservation_datetime_and_coordinates() throws Exception {
            mvc.perform(patch("/api/post/reservation/7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"roomId":8,"scheduledAt":"2026-01-02T03:04:05","placeName":"광산구청","address":"광주 광산구","latitude":35.139,"longitude":126.793}
                                    """))
                    .andExpect(status().isOk()).andExpect(content().string(""));

            verify(reservationProductService).execute(7L, 8L,
                    LocalDateTime.of(2026, 1, 2, 3, 4, 5), "광산구청", "광주 광산구", 35.139, 126.793);
        }

        @Test
        void it_deletes_product_with_no_content() throws Exception {
            mvc.perform(delete("/api/post/7"))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));

            verify(deleteProductByIdService).execute(7L);
        }

        @Test
        void it_deletes_reservation_with_no_content() throws Exception {
            mvc.perform(delete("/api/post/reservation/7"))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));

            verify(deleteReservationProductService).execute(7L);
        }

        @Test
        void it_maps_malformed_trade_body_without_service() throws Exception {
            mvc.perform(post("/api/post/trade").contentType(MediaType.APPLICATION_JSON).content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(requestTradeCompleteService);
        }

        @Test
        void it_maps_malformed_reservation_body_without_service() throws Exception {
            mvc.perform(patch("/api/post/reservation/7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"roomId\":8,\"scheduledAt\":\"not-a-date\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(reservationProductService);
        }
    }
}
