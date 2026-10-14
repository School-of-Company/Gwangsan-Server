package team.startup.gwangsan.adapter.web.trade.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.adapter.web.trade.dto.RequestTradeCompleteRequest;
import team.startup.gwangsan.adapter.web.trade.dto.WithdrawTradeCompleteRequest;
import team.startup.gwangsan.application.trade.data.TradeCompletionCommand;
import team.startup.gwangsan.application.trade.port.in.RequestTradeCompletionUseCase;
import team.startup.gwangsan.application.trade.port.in.WithdrawTradeCompletionUseCase;
import team.startup.gwangsan.domain.common.ActorContext;

/** 거래 완료 요청/확정/요청 취소. 기존 URL(/api/post/trade)과 204 응답을 유지한다. */
@RestController
@RequestMapping("api/post")
@RequiredArgsConstructor
public class TradeCompletionController {

    private final RequestTradeCompletionUseCase requestTradeCompletionUseCase;
    private final WithdrawTradeCompletionUseCase withdrawTradeCompletionUseCase;

    @PostMapping("/trade")
    public ResponseEntity<Void> tradeRequest(ActorContext actor, @RequestBody @Valid RequestTradeCompleteRequest request) {
        requestTradeCompletionUseCase.execute(new TradeCompletionCommand(actor.memberId(), request.productId(), request.otherMemberId()));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/trade")
    public ResponseEntity<Void> tradeWithdraw(ActorContext actor, @RequestBody @Valid WithdrawTradeCompleteRequest request) {
        withdrawTradeCompletionUseCase.execute(new TradeCompletionCommand(actor.memberId(), request.productId(), request.otherMemberId()));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
