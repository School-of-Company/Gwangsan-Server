package team.startup.gwangsan.application.trade.port.in;

import team.startup.gwangsan.application.trade.data.TradeCompletionCommand;

public interface WithdrawTradeCompletionUseCase {
    void execute(TradeCompletionCommand command);
}
