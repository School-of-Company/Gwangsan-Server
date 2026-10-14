package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.trade.data.PlaceTradeCount;
import team.startup.gwangsan.application.trade.data.TradePlace;
import team.startup.gwangsan.application.trade.port.in.TradeStatisticsQuery;
import team.startup.gwangsan.application.trade.port.out.TradePlacePort;
import team.startup.gwangsan.application.trade.port.out.TradeStatisticsPort;
import team.startup.gwangsan.domain.trade.exception.InvalidTradeStatisticsPeriodException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// ponytail: 기존 통계 서비스처럼 트랜잭션을 열지 않는다.
@Service
@RequiredArgsConstructor
public class TradeStatisticsService implements TradeStatisticsQuery {

    private final TradeStatisticsPort tradeStatisticsPort;
    private final TradePlacePort tradePlacePort;

    @Override
    public List<PlaceTradeCount> countByHead(int periodDays, Integer headId) {
        return withPlaces(tradeStatisticsPort.countByHead(periodDays, LocalDateTime.now(), headId));
    }

    @Override
    public List<PlaceTradeCount> countByHead(Integer headId, LocalDate startDate, LocalDate endDate) {
        validatePeriod(startDate, endDate);
        return withPlaces(tradeStatisticsPort.countByHead(headId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()));
    }

    @Override
    public Long countByPlace(int periodDays, Integer placeId) {
        return tradeStatisticsPort.countByPlace(periodDays, LocalDateTime.now(), placeId);
    }

    @Override
    public Long countByPlace(Integer placeId, LocalDate startDate, LocalDate endDate) {
        validatePeriod(startDate, endDate);
        return tradeStatisticsPort.countByPlace(placeId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private List<PlaceTradeCount> withPlaces(Map<Integer, Long> counts) {
        Map<Integer, TradePlace> places = tradePlacePort.findPlaces(counts.keySet());
        return counts.entrySet().stream()
                .map(e -> new PlaceTradeCount(places.get(e.getKey()), e.getValue()))
                .toList();
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new InvalidTradeStatisticsPeriodException();
        }
    }
}
