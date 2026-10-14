package team.startup.gwangsan.adapter.web.trade.dto;

public record WithdrawTradeCompleteRequest(
        Long productId,
        Long otherMemberId
) {
}
