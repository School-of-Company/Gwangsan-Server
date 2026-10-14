package team.startup.gwangsan.adapter.web.trade.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.adapter.web.trade.dto.*;
import team.startup.gwangsan.application.trade.data.*;
import team.startup.gwangsan.application.trade.port.in.RequestTradeCancelUseCase;
import team.startup.gwangsan.application.trade.port.in.TradeHistoryQuery;
import team.startup.gwangsan.application.trade.port.in.TradeStatisticsQuery;
import team.startup.gwangsan.application.trade.port.in.WithdrawTradeCancelUseCase;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.trade.model.TradeStatus;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trade")
public class TradeController {

    private final TradeHistoryQuery tradeHistoryQuery;
    private final TradeStatisticsQuery tradeStatisticsQuery;
    private final RequestTradeCancelUseCase requestTradeCancelUseCase;
    private final WithdrawTradeCancelUseCase withdrawTradeCancelUseCase;

    @GetMapping("/history")
    public ResponseEntity<List<GetTradeHistoryResponse>> getMyTradeHistory(
            ActorContext actor,
            @RequestParam(name = "status", defaultValue = "COMPLETED") TradeStatus status
    ) {
        List<GetTradeHistoryResponse> response = tradeHistoryQuery.query(new TradeHistoryCriteria(actor.memberId(), status))
                .stream()
                .map(TradeController::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/graph/head")
    public ResponseEntity<List<HeadTradeHistoryResponse>> getHeadHistory(
            @RequestParam(name = "period") Period period,
            @RequestParam(name = "head_id") Integer headId
    ) {
        return ResponseEntity.ok(toResponse(tradeStatisticsQuery.countByHead(period.getValue(), headId)));
    }

    @GetMapping("/graph/place")
    public ResponseEntity<PlaceTradeHistoryResponse> getPlaceHistory(
            @RequestParam(name = "period") Period period,
            @RequestParam(name = "place_id") Integer placeId
    ) {
        return ResponseEntity.ok(new PlaceTradeHistoryResponse(tradeStatisticsQuery.countByPlace(period.getValue(), placeId)));
    }

    @GetMapping("/statistics/head")
    public ResponseEntity<List<HeadTradeHistoryResponse>> getHeadStatistics(
            @RequestParam(name = "head_id") Integer headId,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(toResponse(tradeStatisticsQuery.countByHead(headId, startDate, endDate)));
    }

    @GetMapping("/statistics/place")
    public ResponseEntity<PlaceTradeHistoryResponse> getPlaceStatistics(
            @RequestParam(name = "place_id") Integer placeId,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(new PlaceTradeHistoryResponse(tradeStatisticsQuery.countByPlace(placeId, startDate, endDate)));
    }

    // ponytail: 기존과 같이 @Valid 없이 받는다(검증 추가는 계약 변경).
    @PostMapping("/cancel/{product_id}")
    public ResponseEntity<TradeCancelResponse> cancel(
            ActorContext actor,
            @PathVariable("product_id") Long productId,
            @RequestBody TradeCancelRequest request
    ) {
        TradeCancelResult result = requestTradeCancelUseCase.execute(
                new RequestTradeCancelCommand(actor.memberId(), productId, request.reason(), request.imageIds()));
        return ResponseEntity.ok(new TradeCancelResponse(result.cancelled()));
    }

    @DeleteMapping("/cancel/{product_id}")
    public ResponseEntity<Void> cancelWithdraw(ActorContext actor, @PathVariable("product_id") Long productId) {
        withdrawTradeCancelUseCase.execute(new WithdrawTradeCancelCommand(actor.memberId(), productId));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private static GetTradeHistoryResponse toResponse(TradeHistoryItem item) {
        TradeImage image = item.image();
        return new GetTradeHistoryResponse(
                item.tradeId(),
                item.actorIsBuyer() ? TradeRole.BUYER : TradeRole.SELLER,
                item.status(),
                item.completedAt(),
                new GetTradeMemberResponse(item.otherMemberId(), item.otherNickname()),
                new GetTradeProductResponse(item.productId(), item.productTitle(), item.gwangsan(),
                        image == null ? null : new GetImageResponse(image.imageId(), image.imageUrl())));
    }

    private static List<HeadTradeHistoryResponse> toResponse(List<PlaceTradeCount> counts) {
        return counts.stream()
                .map(c -> new HeadTradeHistoryResponse(
                        c.place() == null ? null : new TradePlaceResponse(c.place().placeId(), c.place().name()),
                        c.tradeCount()))
                .toList();
    }
}
