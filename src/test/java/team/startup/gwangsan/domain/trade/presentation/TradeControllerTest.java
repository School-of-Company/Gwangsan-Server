package team.startup.gwangsan.domain.trade.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.exception.TradeParticipantOnlyException;
import team.startup.gwangsan.domain.trade.presentation.dto.request.constant.Period;
import team.startup.gwangsan.domain.trade.presentation.dto.response.PlaceTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.presentation.dto.response.TradeCancelResponse;
import team.startup.gwangsan.domain.trade.service.FindMyTradeHistoryService;
import team.startup.gwangsan.domain.trade.service.TradeCancelService;
import team.startup.gwangsan.domain.trade.service.TradeCancelWithdrawService;
import team.startup.gwangsan.domain.trade.service.TradeHistoryByHeadService;
import team.startup.gwangsan.domain.trade.service.TradeHistoryByPlaceService;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("거래 HTTP 계약")
class TradeControllerTest {

    private MockMvc mvc;
    private FindMyTradeHistoryService findMyTradeHistoryService;
    private TradeHistoryByHeadService tradeHistoryByHeadService;
    private TradeHistoryByPlaceService tradeHistoryByPlaceService;
    private TradeCancelService tradeCancelService;
    private TradeCancelWithdrawService tradeCancelWithdrawService;

    @BeforeEach
    void setUp() {
        findMyTradeHistoryService = mock(FindMyTradeHistoryService.class);
        tradeHistoryByHeadService = mock(TradeHistoryByHeadService.class);
        tradeHistoryByPlaceService = mock(TradeHistoryByPlaceService.class);
        tradeCancelService = mock(TradeCancelService.class);
        tradeCancelWithdrawService = mock(TradeCancelWithdrawService.class);
        mvc = MockMvcBuilders.standaloneSetup(new TradeController(
                        tradeHistoryByHeadService,
                        tradeHistoryByPlaceService,
                        tradeCancelService,
                        tradeCancelWithdrawService,
                        findMyTradeHistoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("거래 이력은")
    class Describe_history {

        @Test
        void it_binds_default_status_enum() throws Exception {
            when(findMyTradeHistoryService.execute(TradeStatus.COMPLETED)).thenReturn(List.of());

            mvc.perform(get("/api/trade/history"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));

            verify(findMyTradeHistoryService).execute(TradeStatus.COMPLETED);
        }

        @Test
        void it_binds_explicit_status_enum() throws Exception {
            when(findMyTradeHistoryService.execute(TradeStatus.CANCELLED)).thenReturn(List.of());

            mvc.perform(get("/api/trade/history").param("status", "CANCELLED"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));

            verify(findMyTradeHistoryService).execute(TradeStatus.CANCELLED);
        }

        @Test
        void it_maps_invalid_status_to_bad_request_without_lookup() throws Exception {
            mvc.perform(get("/api/trade/history").param("status", "complete"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

            verifyNoInteractions(findMyTradeHistoryService);
        }
    }

    @Nested
    @DisplayName("거래 통계는")
    class Describe_statistics {

        @Test
        void it_binds_explicit_period_and_head_id() throws Exception {
            when(tradeHistoryByHeadService.execute(Period.MONTH, 7)).thenReturn(List.of());

            mvc.perform(get("/api/trade/graph/head")
                            .param("period", "MONTH")
                            .param("head_id", "7"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));

            verify(tradeHistoryByHeadService).execute(Period.MONTH, 7);
        }

        @Test
        void it_binds_period_for_place_graph() throws Exception {
            when(tradeHistoryByPlaceService.execute(Period.WEEK, 3))
                    .thenReturn(new PlaceTradeHistoryResponse(2L));

            mvc.perform(get("/api/trade/graph/place").param("period", "WEEK").param("place_id", "3"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(2));

            verify(tradeHistoryByPlaceService).execute(Period.WEEK, 3);
        }

        @Test
        void it_binds_iso_dates_for_head_statistics() throws Exception {
            when(tradeHistoryByHeadService.execute(
                    4, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))).thenReturn(List.of());

            mvc.perform(get("/api/trade/statistics/head")
                            .param("head_id", "4")
                            .param("start_date", "2026-02-01")
                            .param("end_date", "2026-02-28"))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));

            verify(tradeHistoryByHeadService).execute(
                    4, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        }

        @Test
        void it_binds_iso_dates_for_place_statistics() throws Exception {
            when(tradeHistoryByPlaceService.execute(
                    3, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                    .thenReturn(new PlaceTradeHistoryResponse(4L));

            mvc.perform(get("/api/trade/statistics/place")
                            .param("place_id", "3")
                            .param("start_date", "2026-01-01")
                            .param("end_date", "2026-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(4));

            verify(tradeHistoryByPlaceService).execute(
                    3, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        }

        @Test
        void it_maps_malformed_dates_to_bad_request_without_statistics_lookup() throws Exception {
            mvc.perform(get("/api/trade/statistics/place")
                            .param("place_id", "3")
                            .param("start_date", "2026/01/01")
                            .param("end_date", "2026-01-31"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

            verifyNoInteractions(tradeHistoryByPlaceService);
        }
    }

    @Nested
    @DisplayName("거래 철회는")
    class Describe_cancel {

        @Test
        void it_returns_cancellation_result_and_binds_request_body() throws Exception {
            when(tradeCancelService.execute(9L, "사유", List.of(1L, 2L)))
                    .thenReturn(new TradeCancelResponse(false));

            mvc.perform(post("/api/trade/cancel/9")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"사유\",\"imageIds\":[1,2]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cancelled").value(false));

            verify(tradeCancelService).execute(9L, "사유", List.of(1L, 2L));
        }

        @Test
        void it_maps_trade_exception_to_its_status_and_body() throws Exception {
            when(tradeCancelService.execute(9L, "사유", List.of(1L)))
                    .thenThrow(new TradeParticipantOnlyException());

            mvc.perform(post("/api/trade/cancel/9")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"사유\",\"imageIds\":[1]}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value("거래 당사자만 요청할 수 있습니다."));
        }

        @Test
        void it_maps_malformed_json_to_bad_request_without_cancellation_service() throws Exception {
            mvc.perform(post("/api/trade/cancel/9")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

            verifyNoInteractions(tradeCancelService);
        }

        @Test
        void it_withdraws_cancellation_with_no_content() throws Exception {
            mvc.perform(delete("/api/trade/cancel/9"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(tradeCancelWithdrawService).execute(9L);
        }
    }
}
