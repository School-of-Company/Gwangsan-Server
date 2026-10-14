package team.startup.gwangsan.application.trade.data;

/** place 는 지점 정보가 없으면 null (기존 응답과 같음). */
public record PlaceTradeCount(TradePlace place, Long tradeCount) {
}
