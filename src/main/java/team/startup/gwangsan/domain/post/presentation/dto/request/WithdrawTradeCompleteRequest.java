package team.startup.gwangsan.domain.post.presentation.dto.request;

public record WithdrawTradeCompleteRequest(
        Long productId,
        Long otherMemberId
) {
}
