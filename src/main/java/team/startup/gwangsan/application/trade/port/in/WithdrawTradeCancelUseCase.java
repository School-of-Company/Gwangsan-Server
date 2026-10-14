package team.startup.gwangsan.application.trade.port.in;

import team.startup.gwangsan.application.trade.data.WithdrawTradeCancelCommand;

public interface WithdrawTradeCancelUseCase {
    void execute(WithdrawTradeCancelCommand command);
}
