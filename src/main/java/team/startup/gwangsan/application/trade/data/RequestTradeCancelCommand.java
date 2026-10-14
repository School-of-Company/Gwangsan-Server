package team.startup.gwangsan.application.trade.data;

import java.util.List;

public record RequestTradeCancelCommand(long actorId, long productId, String reason, List<Long> imageIds) {
}
