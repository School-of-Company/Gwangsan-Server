package team.startup.gwangsan.adapter.web.post.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import team.startup.gwangsan.application.post.data.CreateProductCommand;
import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.data.MemberProductsCriteria;
import team.startup.gwangsan.application.post.data.ProductAuthor;
import team.startup.gwangsan.application.post.data.ProductDetailView;
import team.startup.gwangsan.application.post.data.ProductView;
import team.startup.gwangsan.application.post.data.ReserveProductCommand;
import team.startup.gwangsan.application.post.data.UpdateProductCommand;
import team.startup.gwangsan.application.post.port.in.CancelReservationUseCase;
import team.startup.gwangsan.application.post.port.in.CreateProductUseCase;
import team.startup.gwangsan.application.post.port.in.DeleteProductUseCase;
import team.startup.gwangsan.application.post.port.in.MemberProductsQuery;
import team.startup.gwangsan.application.post.port.in.PlaceProductsQuery;
import team.startup.gwangsan.application.post.port.in.ProductDetailQuery;
import team.startup.gwangsan.application.post.port.in.ReserveProductUseCase;
import team.startup.gwangsan.application.post.port.in.UpdateProductUseCase;
import team.startup.gwangsan.application.post.port.in.ValidateProductContentUseCase;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.post.exception.InappropriateContentException;
import team.startup.gwangsan.infrastructure.web.GlobalExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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

    private static final Long ACTOR = 1L;

    private MockMvc mvc;
    private ValidateProductContentUseCase contentValidationService;
    private CreateProductUseCase createProductService;
    private UpdateProductUseCase updateProductService;
    private DeleteProductUseCase deleteProductByIdService;
    private ProductDetailQuery findProductByIdService;
    private PlaceProductsQuery findProductsByTypeAndModeService;
    private MemberProductsQuery memberProductsQuery;
    private ReserveProductUseCase reservationProductService;
    private CancelReservationUseCase deleteReservationProductService;

    @BeforeEach
    void setUp() {
        contentValidationService = mock(ValidateProductContentUseCase.class);
        createProductService = mock(CreateProductUseCase.class);
        updateProductService = mock(UpdateProductUseCase.class);
        deleteProductByIdService = mock(DeleteProductUseCase.class);
        findProductByIdService = mock(ProductDetailQuery.class);
        findProductsByTypeAndModeService = mock(PlaceProductsQuery.class);
        memberProductsQuery = mock(MemberProductsQuery.class);
        reservationProductService = mock(ReserveProductUseCase.class);
        deleteReservationProductService = mock(CancelReservationUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new PostController(
                        contentValidationService,
                        createProductService,
                        updateProductService,
                        deleteProductByIdService,
                        findProductByIdService,
                        findProductsByTypeAndModeService,
                        memberProductsQuery,
                        reservationProductService,
                        deleteReservationProductService))
                .setCustomArgumentResolvers(new FixedActorResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // 실제 앱에서는 ActorContextArgumentResolver 가 인증 principal 로 채운다.
    private static class FixedActorResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return ActorContext.class.equals(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return new ActorContext(ACTOR, ActorContext.Role.ROLE_USER);
        }
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
            order.verify(contentValidationService).execute("자전거 수리", "브레이크를 고쳐 드립니다.");
            order.verify(createProductService).execute(new CreateProductCommand(ACTOR, Type.OBJECT, Mode.GIVER,
                    "자전거 수리", "브레이크를 고쳐 드립니다.", 5000, List.of(11L, 12L)));
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
                    .execute("금지 제목", "금지 내용");

            mvc.perform(post("/api/post")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type":"SERVICE","mode":"RECEIVER","title":"금지 제목","content":"금지 내용","gwangsan":5000}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("부적절한 내용이 포함되어 있습니다."));

            verify(contentValidationService).execute("금지 제목", "금지 내용");
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
            order.verify(contentValidationService).execute("이사 도움", "짐을 옮겨 드립니다.");
            order.verify(updateProductService).execute(new UpdateProductCommand(ACTOR, 7L, Type.SERVICE,
                    Mode.RECEIVER, "이사 도움", "짐을 옮겨 드립니다.", 10000, List.of(4L)));
        }

        @Test
        void it_maps_moderation_rejection_without_updating() throws Exception {
            doThrow(new InappropriateContentException()).when(contentValidationService)
                    .execute("금지 제목", "금지 내용");

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
            when(findProductByIdService.query(ACTOR, 7L)).thenReturn(new ProductDetailView(
                    7L, "수리", "설명", 5000, Type.OBJECT, Mode.GIVER, new ProductAuthor(2L, "작성자", "광산", 3),
                    List.of(new ImageView(5L, "https://img/5")), true, false, false, true));
            mvc.perform(get("/api/post/7"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("""
                            {"id":7,"title":"수리","content":"설명","gwangsan":5000,"type":"OBJECT","mode":"GIVER",
                             "member":{"memberId":2,"nickname":"작성자","placeName":"광산","light":3},
                             "images":[{"imageId":5,"imageUrl":"https://img/5"}],
                             "isMine":true,"isCompletable":false,"isCompleted":false,"isReserved":true}
                            """, true));

            verify(findProductByIdService).query(ACTOR, 7L);
        }

        @Test
        void it_binds_explicit_listing_filters() throws Exception {
            when(findProductsByTypeAndModeService.query(ACTOR, Type.OBJECT, Mode.GIVER)).thenReturn(List.of(
                    new ProductView(8L, "제목", "내용", 1000, Type.OBJECT, Mode.GIVER,
                            new ProductAuthor(2L, "작성자", "광산", 1), List.of(), true, false)));

            mvc.perform(get("/api/post").param("type", "OBJECT").param("mode", "GIVER"))
                    .andExpect(status().isOk()).andExpect(content().json("""
                            [{"id":8,"title":"제목","content":"내용","gwangsan":1000,"type":"OBJECT","mode":"GIVER",
                              "member":{"memberId":2,"nickname":"작성자","placeName":"광산","light":1},
                              "images":[],"isCompleted":true,"isReserved":false}]
                            """, true));

            verify(findProductsByTypeAndModeService).query(ACTOR, Type.OBJECT, Mode.GIVER);
        }

        @Test
        void it_binds_omitted_current_listing_filters_as_null() throws Exception {
            when(memberProductsQuery.query(any())).thenReturn(List.of());

            mvc.perform(get("/api/post/current"))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));

            verify(memberProductsQuery).query(new MemberProductsCriteria(ACTOR, null, null, null, null, null));
        }

        @Test
        void it_binds_member_listing_with_omitted_mode_as_null() throws Exception {
            when(memberProductsQuery.query(any())).thenReturn(List.of());

            mvc.perform(get("/api/post/member/8").param("type", "SERVICE"))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));

            verify(memberProductsQuery).query(new MemberProductsCriteria(8L, Type.SERVICE, null, null, null, null));
        }

        @Test
        void it_binds_current_page_parameters_to_actor_criteria() throws Exception {
            when(memberProductsQuery.query(any())).thenReturn(List.of());

            mvc.perform(get("/api/post/current").param("size", "2").param("last_id", "9").param("completed", "false"))
                    .andExpect(status().isOk());

            verify(memberProductsQuery).query(new MemberProductsCriteria(ACTOR, null, null, 9L, 2, false));
        }

        @Test
        void it_rejects_invalid_page_parameters_with_empty_400_without_query() throws Exception {
            for (String query : List.of("size=0", "size=101", "last_id=2", "completed=true", "size=2&last_id=0")) {
                mvc.perform(get("/api/post/current?" + query))
                        .andExpect(status().isBadRequest()).andExpect(content().string(""));
                mvc.perform(get("/api/post/member/8?" + query))
                        .andExpect(status().isBadRequest()).andExpect(content().string(""));
            }
            verifyNoInteractions(memberProductsQuery);
        }
    }

    @Nested
    @DisplayName("예약과 삭제 요청은")
    class Describe_trade_and_reservation {

        @Test
        void it_binds_reservation_datetime_and_coordinates() throws Exception {
            mvc.perform(patch("/api/post/reservation/7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"roomId":8,"scheduledAt":"2026-01-02T03:04:05","placeName":"광산구청","address":"광주 광산구","latitude":35.139,"longitude":126.793}
                                    """))
                    .andExpect(status().isOk()).andExpect(content().string(""));

            verify(reservationProductService).execute(new ReserveProductCommand(ACTOR, 7L, 8L,
                    LocalDateTime.of(2026, 1, 2, 3, 4, 5), "광산구청", "광주 광산구", 35.139, 126.793));
        }

        @Test
        void it_deletes_product_with_no_content() throws Exception {
            mvc.perform(delete("/api/post/7"))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));

            verify(deleteProductByIdService).execute(ACTOR, 7L);
        }

        @Test
        void it_deletes_reservation_with_no_content() throws Exception {
            mvc.perform(delete("/api/post/reservation/7"))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));

            verify(deleteReservationProductService).execute(ACTOR, 7L);
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
