package team.startup.gwangsan.adapter.integration.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;
import team.startup.gwangsan.application.trade.data.TradePlace;
import team.startup.gwangsan.application.trade.port.out.TradePlacePort;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** trade → place 공개 query 연결. */
@Component
@RequiredArgsConstructor
public class TradePlaceAdapter implements TradePlacePort {

    private final PlaceQuery placeQuery;

    @Override
    public Map<Integer, TradePlace> findPlaces(Collection<Integer> placeIds) {
        return placeQuery.findAllByIds(placeIds).stream()
                .map(p -> new TradePlace(p.id(), p.name()))
                .collect(Collectors.toMap(TradePlace::placeId, Function.identity()));
    }
}
