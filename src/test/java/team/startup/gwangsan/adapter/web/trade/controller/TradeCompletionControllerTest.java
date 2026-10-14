package team.startup.gwangsan.adapter.web.trade.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.adapter.web.support.FixedActorResolver;
import team.startup.gwangsan.application.trade.data.TradeCompletionCommand;
import team.startup.gwangsan.application.trade.port.in.RequestTradeCompletionUseCase;
import team.startup.gwangsan.application.trade.port.in.WithdrawTradeCompletionUseCase;
import team.startup.gwangsan.domain.trade.exception.TradeAlreadyCompleteRequestException;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("거래 완료 HTTP 계약 (/api/post/trade)")
class TradeCompletionControllerTest {

    private static final long ACTOR = 3L;

    private MockMvc mvc;
    private RequestTradeCompletionUseCase requestUseCase;
    private WithdrawTradeCompletionUseCase withdrawUseCase;

    @BeforeEach
    void setUp() {
        requestUseCase = mock(RequestTradeCompletionUseCase.class);
        withdrawUseCase = mock(WithdrawTradeCompletionUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new TradeCompletionController(requestUseCase, withdrawUseCase))
                .setCustomArgumentResolvers(FixedActorResolver.user(ACTOR))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void it_binds_trade_request_body_with_authenticated_actor_and_returns_no_content() throws Exception {
        mvc.perform(post("/api/post/trade").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":7,\"otherMemberId\":8}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));

        verify(requestUseCase).execute(new TradeCompletionCommand(ACTOR, 7L, 8L));
    }

    @Test
    void it_binds_trade_withdraw_body_and_returns_no_content() throws Exception {
        mvc.perform(delete("/api/post/trade").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":7,\"otherMemberId\":8}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));

        verify(withdrawUseCase).execute(new TradeCompletionCommand(ACTOR, 7L, 8L));
    }

    @Test
    void it_maps_malformed_trade_body_without_use_case() throws Exception {
        mvc.perform(post("/api/post/trade").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(requestUseCase);
    }

    @Test
    void it_maps_trade_exception_to_its_status() throws Exception {
        doThrow(new TradeAlreadyCompleteRequestException()).when(requestUseCase).execute(any());

        mvc.perform(post("/api/post/trade").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":7,\"otherMemberId\":8}"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(status().is4xxClientError());
    }
}
