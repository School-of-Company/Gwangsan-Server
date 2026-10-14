package team.startup.archfixture.legal.adapter.persistence.order;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.archfixture.legal.application.order.port.out.OrderPort;
import team.startup.archfixture.legal.domain.order.model.Order;

import java.util.Optional;

interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {
}

public class OrderPersistenceAdapter implements OrderPort {

    private final OrderJpaRepository repository;

    OrderPersistenceAdapter(OrderJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Order> loadForUpdate(Long id) {
        return repository.findById(id).map(e -> new Order(e.id, e.ownerId, e.confirmed));
    }

    @Override
    public void save(Order order) {
        OrderJpaEntity entity = repository.findById(order.id()).orElseThrow();
        entity.confirmed = order.confirmed();
    }
}
