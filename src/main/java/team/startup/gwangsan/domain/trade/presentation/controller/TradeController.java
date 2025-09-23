package team.startup.gwangsan.domain.trade.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.domain.trade.presentation.dto.request.constant.Period;
import team.startup.gwangsan.domain.trade.presentation.dto.response.HeadTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.presentation.dto.response.PlaceTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.service.TradeHistoryByHeadService;
import team.startup.gwangsan.domain.trade.service.TradeHistoryByPlaceService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trade")
public class TradeController {

    private final TradeHistoryByHeadService tradeHistoryByHeadService;
    private final TradeHistoryByPlaceService tradeHistoryByPlaceService;

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
}
