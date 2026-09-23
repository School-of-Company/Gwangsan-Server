package team.startup.gwangsan.domain.post.service;

public interface WithdrawTradeCompleteService {
    void execute(Long productId, Long otherMemberId);
}
