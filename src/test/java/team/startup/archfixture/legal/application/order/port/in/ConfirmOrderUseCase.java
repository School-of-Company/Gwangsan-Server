package team.startup.archfixture.legal.application.order.port.in;

import team.startup.archfixture.legal.application.order.data.ConfirmOrderCommand;

public interface ConfirmOrderUseCase {
    void execute(ConfirmOrderCommand command);
}
