package team.startup.gwangsan.adapter.web.trade.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.adapter.web.support.FixedActorResolver;
import team.startup.gwangsan.application.trade.data.*;
import team.startup.gwangsan.application.trade.port.in.RequestTradeCancelUseCase;
import team.startup.gwangsan.application.trade.port.in.TradeHistoryQuery;
import team.startup.gwangsan.application.trade.port.in.TradeStatisticsQuery;
import team.startup.gwangsan.application.trade.port.in.WithdrawTradeCancelUseCase;
import team.startup.gwangsan.domain.trade.exception.TradeParticipantOnlyException;
import team.startup.gwangsan.domain.trade.model.TradeStatus;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("거래 HTTP 계약")
class TradeControllerTest {

    private static final long ACTOR = 1L;

    private MockMvc mvc;
    private TradeHistoryQuery tradeHistoryQuery;
    private TradeStatisticsQuery tradeStatisticsQuery;
    private RequestTradeCancelUseCase requestTradeCancelUseCase;
    private WithdrawTradeCancelUseCase withdrawTradeCancelUseCase;

    @BeforeEach
    void setUp() {
        tradeHistoryQuery = mock(TradeHistoryQuery.class);
        tradeStatisticsQuery = mock(TradeStatisticsQuery.class);
        requestTradeCancelUseCase = mock(RequestTradeCancelUseCase.class);
        withdrawTradeCancelUseCase = mock(WithdrawTradeCancelUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new TradeController(
                        tradeHistoryQuery, tradeStatisticsQuery, requestTradeCancelUseCase, withdrawTradeCancelUseCase))
                .setCustomArgumentResolvers(FixedActorResolver.user(ACTOR))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("거래 이력은")
    class Describe_history {

        @Test
        void it_binds_default_status_enum() throws Exception {
            when(tradeHistoryQuery.query(new TradeHistoryCriteria(ACTOR, TradeStatus.COMPLETED))).thenReturn(List.of());

            mvc.perform(get("/api/trade/history"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));

            verify(tradeHistoryQuery).query(new TradeHistoryCriteria(ACTOR, TradeStatus.COMPLETED));
        }

        @Test
        void it_binds_explicit_status_enum() throws Exception {
            when(tradeHistoryQuery.query(new TradeHistoryCriteria(ACTOR, TradeStatus.CANCELLED))).thenReturn(List.of());

            mvc.perform(get("/api/trade/history").param("status", "CANCELLED"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        void it_keeps_response_field_names_and_null_image() throws Exception {
            when(tradeHistoryQuery.query(new TradeHistoryCriteria(ACTOR, TradeStatus.COMPLETED))).thenReturn(List.of(
                    new TradeHistoryItem(10L, true, TradeStatus.COMPLETED, LocalDateTime.of(2026, 1, 2, 3, 4, 5),
                            2L, "상대", 100L, "상품", 3000, new TradeImage(7L, "https://img")),
                    new TradeHistoryItem(11L, false, TradeStatus.COMPLETED, null, 3L, "구매자", 101L, "상품2", 1000, null)));

            mvc.perform(get("/api/trade/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].tradeId").value(10))
                    .andExpect(jsonPath("$[0].role").value("BUYER"))
                    .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                    .andExpect(jsonPath("$[0].completedAt").exists())
                    .andExpect(jsonPath("$[0].otherMember.memberId").value(2))
                    .andExpect(jsonPath("$[0].otherMember.nickname").value("상대"))
                    .andExpect(jsonPath("$[0].product.productId").value(100))
                    .andExpect(jsonPath("$[0].product.title").value("상품"))
                    .andExpect(jsonPath("$[0].product.gwangsan").value(3000))
                    .andExpect(jsonPath("$[0].product.image.imageId").value(7))
                    .andExpect(jsonPath("$[0].product.image.imageUrl").value("https://img"))
                    .andExpect(jsonPath("$[1].role").value("SELLER"))
                    .andExpect(jsonPath("$[1].product.image").isEmpty());
        }

        @Test
        void it_maps_invalid_status_to_bad_request_without_lookup() throws Exception {
            mvc.perform(get("/api/trade/history").param("status", "complete"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

            verifyNoInteractions(tradeHistoryQuery);
        }
    }

    @Nested
    @DisplayName("거래 통계는")
    class Describe_statistics {

        @Test
        void it_binds_period_as_days_and_keeps_head_response_shape() throws Exception {
            when(tradeStatisticsQuery.countByHead(30, 7)).thenReturn(List.of(
                    new PlaceTradeCount(new TradePlace(2, "첨단"), 5L), new PlaceTradeCount(null, 1L)));

            mvc.perform(get("/api/trade/graph/head").param("period", "MONTH").param("head_id", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].place.id").value(2))
                    .andExpect(jsonPath("$[0].place.name").value("첨단"))
                    .andExpect(jsonPath("$[0].tradeCount").value(5))
                    .andExpect(jsonPath("$[1].place").isEmpty())
                    .andExpect(jsonPath("$[1].tradeCount").value(1));
        }

        @Test
        void it_binds_period_for_place_graph() throws Exception {
            when(tradeStatisticsQuery.countByPlace(7, 3)).thenReturn(2L);

            mvc.perform(get("/api/trade/graph/place").param("period", "WEEK").param("place_id", "3"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(2));
        }

        @Test
        void it_binds_iso_dates_for_head_statistics() throws Exception {
            when(tradeStatisticsQuery.countByHead(4, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))).thenReturn(List.of());

            mvc.perform(get("/api/trade/statistics/head")
                            .param("head_id", "4").param("start_date", "2026-02-01").param("end_date", "2026-02-28"))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));

            verify(tradeStatisticsQuery).countByHead(4, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        }

        @Test
        void it_binds_iso_dates_for_place_statistics() throws Exception {
            when(tradeStatisticsQuery.countByPlace(3, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))).thenReturn(4L);

            mvc.perform(get("/api/trade/statistics/place")
                            .param("place_id", "3").param("start_date", "2026-01-01").param("end_date", "2026-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(4));
        }

        @Test
        void it_maps_malformed_dates_to_bad_request_without_statistics_lookup() throws Exception {
            mvc.perform(get("/api/trade/statistics/place")
                            .param("place_id", "3").param("start_date", "2026/01/01").param("end_date", "2026-01-31"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

            verifyNoInteractions(tradeStatisticsQuery);
        }
    }

    @Nested
    @DisplayName("거래 철회는")
    class Describe_cancel {

        @Test
        void it_returns_cancellation_result_and_binds_request_body() throws Exception {
            when(requestTradeCancelUseCase.execute(new RequestTradeCancelCommand(ACTOR, 9L, "사유", List.of(1L, 2L))))
                    .thenReturn(new TradeCancelResult(false));

            mvc.perform(post("/api/trade/cancel/9")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"사유\",\"imageIds\":[1,2]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cancelled").value(false));
        }

        @Test
        void it_maps_trade_exception_to_its_status_and_body() throws Exception {
            when(requestTradeCancelUseCase.execute(new RequestTradeCancelCommand(ACTOR, 9L, "사유", List.of(1L))))
                    .thenThrow(new TradeParticipantOnlyException());

            mvc.perform(post("/api/trade/cancel/9")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"사유\",\"imageIds\":[1]}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value("거래 당사자만 요청할 수 있습니다."));
        }

        @Test
        void it_maps_malformed_json_to_bad_request_without_cancellation() throws Exception {
            mvc.perform(post("/api/trade/cancel/9")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

            verifyNoInteractions(requestTradeCancelUseCase);
        }

        @Test
        void it_withdraws_cancellation_with_no_content() throws Exception {
            mvc.perform(delete("/api/trade/cancel/9"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(withdrawTradeCancelUseCase).execute(new WithdrawTradeCancelCommand(ACTOR, 9L));
        }
    }
}
