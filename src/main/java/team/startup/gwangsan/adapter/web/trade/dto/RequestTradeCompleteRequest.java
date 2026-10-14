package team.startup.gwangsan.adapter.web.trade.dto;

public record RequestTradeCompleteRequest(
        Long productId,
        Long otherMemberId
) {
}
