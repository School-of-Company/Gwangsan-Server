package team.startup.gwangsan.domain.block.model;

import team.startup.gwangsan.domain.block.exception.SelfBlockNotAllowedException;

public record MemberBlock(Long blockerId, Long blockedId) {

    public static MemberBlock of(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new SelfBlockNotAllowedException();
        }
        return new MemberBlock(blockerId, blockedId);
    }
}
