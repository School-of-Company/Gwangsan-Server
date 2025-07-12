package team.startup.gwangsan.domain.post.presentation.dto.request;

public record RequestTradeCompleteRequest(
        Long productId,
        Long otherMemberId
) {
}
