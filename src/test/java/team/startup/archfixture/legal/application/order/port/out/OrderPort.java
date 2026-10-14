package team.startup.archfixture.legal.application.order.port.out;

import team.startup.archfixture.legal.domain.order.model.Order;

import java.util.Optional;

public interface OrderPort {
    Optional<Order> loadForUpdate(Long id);

    void save(Order order);
}
