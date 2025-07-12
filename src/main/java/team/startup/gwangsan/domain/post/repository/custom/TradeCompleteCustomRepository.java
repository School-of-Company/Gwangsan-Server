package team.startup.gwangsan.domain.post.repository.custom;

public interface TradeCompleteCustomRepository {
    long countMutualTradeComplete(Long productId, Long memberId1, Long memberId2);
}
