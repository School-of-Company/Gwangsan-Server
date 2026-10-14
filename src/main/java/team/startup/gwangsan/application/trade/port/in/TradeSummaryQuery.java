package team.startup.gwangsan.application.trade.port.in;

import team.startup.gwangsan.application.trade.data.TradeCancelParties;
import team.startup.gwangsan.application.trade.data.TradeCompleteSummary;

/** 거래·철회 요약 공개 조회(알림 원천 등). 없으면 거래 업무의 기존 NotFound 예외를 던진다. */
public interface TradeSummaryQuery {
    /** 없으면 NotFoundTradeCompleteException. 기존 경로와 같이 거래 1회 + 상품·구매자 지연 로딩. */
    TradeCompleteSummary getTradeComplete(long tradeCompleteId);

    /** 없으면 NotFoundTradeCancelException. 기존 경로와 같이 거래·당사자 fetch join 1회. */
    TradeCancelParties getCancelParties(long tradeCancelId);
}
