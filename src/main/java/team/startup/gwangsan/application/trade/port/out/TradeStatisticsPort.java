package team.startup.gwangsan.application.trade.port.out;

import java.time.LocalDateTime;
import java.util.Map;

/** 지점 키별 완료 거래 수. 키 순서는 저장소 결과 순서를 유지한다. */
public interface TradeStatisticsPort {
    Map<Integer, Long> countByHead(int periodDays, LocalDateTime now, Integer headId);

    Map<Integer, Long> countByHead(Integer headId, LocalDateTime start, LocalDateTime end);

    Long countByPlace(int periodDays, LocalDateTime now, Integer placeId);

    Long countByPlace(Integer placeId, LocalDateTime start, LocalDateTime end);
}
