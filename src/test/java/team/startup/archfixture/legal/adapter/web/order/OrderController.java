package team.startup.archfixture.legal.adapter.web.order;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import team.startup.archfixture.legal.application.order.data.ConfirmOrderCommand;
import team.startup.archfixture.legal.application.order.port.in.ConfirmOrderUseCase;
import team.startup.archfixture.legal.domain.common.Actor;

@RestController
public class OrderController {

    private final ConfirmOrderUseCase confirmOrderUseCase;

    public OrderController(ConfirmOrderUseCase confirmOrderUseCase) {
        this.confirmOrderUseCase = confirmOrderUseCase;
    }

    @PostMapping("/api/order/{orderId}/confirm")
    public void confirm(Actor actor, @PathVariable Long orderId) {
        confirmOrderUseCase.execute(new ConfirmOrderCommand(actor, orderId));
    }
}
