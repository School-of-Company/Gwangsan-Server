package team.startup.gwangsan.domain.post.repository.custom;

import java.time.LocalDateTime;
import java.util.Map;

public interface TradeCompleteCustomRepository {
    Map<Integer, Long> countByHeadId(int period, LocalDateTime now, Integer headId);

    Long countByPlaceId(int period, LocalDateTime now, Integer placeId);
}
