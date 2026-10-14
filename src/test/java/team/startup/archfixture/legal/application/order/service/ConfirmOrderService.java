package team.startup.archfixture.legal.application.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.archfixture.legal.application.order.data.ConfirmOrderCommand;
import team.startup.archfixture.legal.application.order.port.in.ConfirmOrderUseCase;
import team.startup.archfixture.legal.application.order.port.out.OrderPort;
import team.startup.archfixture.legal.domain.order.model.Order;

@Service
public class ConfirmOrderService implements ConfirmOrderUseCase {

    private final OrderPort orderPort;

    public ConfirmOrderService(OrderPort orderPort) {
        this.orderPort = orderPort;
    }

    @Override
    @Transactional
    public void execute(ConfirmOrderCommand command) {
        Order order = orderPort.loadForUpdate(command.orderId()).orElseThrow();
        orderPort.save(order.confirm(command.actor()));
    }
}
