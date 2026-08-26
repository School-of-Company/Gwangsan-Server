package team.startup.gwangsan.domain.trade.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.trade.entity.constant.TradeStatus;
import team.startup.gwangsan.domain.trade.presentation.dto.request.TradeCancelRequest;
import team.startup.gwangsan.domain.trade.presentation.dto.request.constant.Period;
import team.startup.gwangsan.domain.trade.presentation.dto.response.GetTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.presentation.dto.response.HeadTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.presentation.dto.response.PlaceTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.service.FindMyTradeHistoryService;
import team.startup.gwangsan.domain.trade.service.TradeCancelService;
import team.startup.gwangsan.domain.trade.service.TradeCancelWithdrawService;
import team.startup.gwangsan.domain.trade.service.TradeHistoryByHeadService;
import team.startup.gwangsan.domain.trade.service.TradeHistoryByPlaceService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trade")
public class TradeController {

    private final TradeHistoryByHeadService tradeHistoryByHeadService;
    private final TradeHistoryByPlaceService tradeHistoryByPlaceService;
    private final TradeCancelService tradeCancelService;
    private final TradeCancelWithdrawService tradeCancelWithdrawService;
    private final FindMyTradeHistoryService findMyTradeHistoryService;

    @GetMapping("/history")
    public ResponseEntity<List<GetTradeHistoryResponse>> getMyTradeHistory(
            @RequestParam(name = "status", defaultValue = "COMPLETED") TradeStatus status
    ) {
        List<GetTradeHistoryResponse> response = findMyTradeHistoryService.execute(status);
        return ResponseEntity.ok(response);
    }

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

    @GetMapping("/statistics/head")
    public ResponseEntity<List<HeadTradeHistoryResponse>> getHeadStatistics(
            @RequestParam(name = "head_id") Integer headId,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<HeadTradeHistoryResponse> response = tradeHistoryByHeadService.execute(headId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics/place")
    public ResponseEntity<PlaceTradeHistoryResponse> getPlaceStatistics(
            @RequestParam(name = "place_id") Integer placeId,
            @RequestParam(name = "start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        PlaceTradeHistoryResponse response = tradeHistoryByPlaceService.execute(placeId, startDate, endDate);
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
