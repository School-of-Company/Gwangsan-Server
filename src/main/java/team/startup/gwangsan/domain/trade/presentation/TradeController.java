package team.startup.gwangsan.domain.trade.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.trade.presentation.dto.request.TradeCancelRequest;
import team.startup.gwangsan.domain.trade.presentation.dto.request.constant.Period;
import team.startup.gwangsan.domain.trade.presentation.dto.response.HeadTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.presentation.dto.response.PlaceTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.service.TradeCancelService;
import team.startup.gwangsan.domain.trade.service.TradeCancelWithdrawService;
import team.startup.gwangsan.domain.trade.service.TradeHistoryByHeadService;
import team.startup.gwangsan.domain.trade.service.TradeHistoryByPlaceService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trade")
public class TradeController {

    private final TradeHistoryByHeadService tradeHistoryByHeadService;
    private final TradeHistoryByPlaceService tradeHistoryByPlaceService;
    private final TradeCancelService tradeCancelService;
    private final TradeCancelWithdrawService tradeCancelWithdrawService;

    @GetMapping("/graph/head")
    public ResponseEntity<List<HeadTradeHistoryResponse>> getHeadHistory(
            @RequestParam(name = "period") Period period,
            @RequestParam(name = "head_id") Integer headId
    ) {
        List<HeadTradeHistoryResponse> response = tradeHistoryByHeadService.execute(period, headId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/graph/place")
    public ResponseEntity<PlaceTradeHistoryResponse> getPlaceHistory(
            @RequestParam(name = "period") Period period,
            @RequestParam(name = "place_id") Integer placeId
    ) {
        PlaceTradeHistoryResponse response = tradeHistoryByPlaceService.execute(period, placeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel/{product_id}")
    public ResponseEntity<Void> cancel(
            @PathVariable("product_id") Long productId,
            @RequestBody TradeCancelRequest request
    ) {
        tradeCancelService.execute(productId, request.reason(), request.imageIds());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cancel/{product_id}")
    public ResponseEntity<Void> cancelWithdraw(@PathVariable("product_id") Long productId) {
        tradeCancelWithdrawService.execute(productId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
