package team.startup.archfixture.legal.application.order.data;

import team.startup.archfixture.legal.domain.common.Actor;

public record ConfirmOrderCommand(Actor actor, Long orderId) {
}
