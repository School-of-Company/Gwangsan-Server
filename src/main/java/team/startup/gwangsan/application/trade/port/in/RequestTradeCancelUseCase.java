package team.startup.gwangsan.application.trade.port.in;

import team.startup.gwangsan.application.trade.data.RequestTradeCancelCommand;
import team.startup.gwangsan.application.trade.data.TradeCancelResult;

public interface RequestTradeCancelUseCase {
    TradeCancelResult execute(RequestTradeCancelCommand command);
}
