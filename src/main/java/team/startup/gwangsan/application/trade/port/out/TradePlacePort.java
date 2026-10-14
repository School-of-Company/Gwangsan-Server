package team.startup.gwangsan.application.trade.port.out;

import team.startup.gwangsan.application.trade.data.TradePlace;

import java.util.Collection;
import java.util.Map;

public interface TradePlacePort {
    Map<Integer, TradePlace> findPlaces(Collection<Integer> placeIds);
}
