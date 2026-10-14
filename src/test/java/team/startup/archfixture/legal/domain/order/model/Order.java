package team.startup.archfixture.legal.domain.order.model;

import team.startup.archfixture.legal.domain.common.Actor;

public record Order(Long id, Long ownerId, boolean confirmed) {

    public Order confirm(Actor actor) {
        if (!ownerId.equals(actor.memberId())) {
            throw new IllegalStateException("owner only");
        }
        return new Order(id, ownerId, true);
    }
}
