package team.startup.gwangsan.domain.post.service;

public interface RequestTradeCompleteService {
    void execute(Long productId, Long otherMemberId);
}
