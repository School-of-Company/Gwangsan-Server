package team.startup.gwangsan.application.trade.port.in;

import team.startup.gwangsan.application.trade.data.TradeCompletionCommand;

/** 게시글 작성자는 즉시 완료한다. 비작성자는 대기 요청이 없으면 완료를 요청하고, 상대방의 대기 요청이 있으면 확정한다. */
public interface RequestTradeCompletionUseCase {
    void execute(TradeCompletionCommand command);
}
