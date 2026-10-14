package team.startup.gwangsan.application.trade.port.in;

import team.startup.gwangsan.application.trade.data.PlaceTradeCount;

import java.time.LocalDate;
import java.util.List;

/** 완료 거래 수 통계. periodDays 는 오늘을 포함한 최근 일수다. */
public interface TradeStatisticsQuery {
    List<PlaceTradeCount> countByHead(int periodDays, Integer headId);

    List<PlaceTradeCount> countByHead(Integer headId, LocalDate startDate, LocalDate endDate);

    Long countByPlace(int periodDays, Integer placeId);

    Long countByPlace(Integer placeId, LocalDate startDate, LocalDate endDate);
}
