package team.startup.gwangsan.application.trade.port.in;

import team.startup.gwangsan.application.trade.data.ApproveTradeCancelCommand;

public interface ApproveTradeCancelUseCase {
    void execute(ApproveTradeCancelCommand command);
}
