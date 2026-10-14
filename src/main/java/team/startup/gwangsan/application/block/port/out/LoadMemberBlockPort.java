package team.startup.gwangsan.application.block.port.out;

import team.startup.gwangsan.application.block.data.BlockedMember;

import java.util.List;

public interface LoadMemberBlockPort {

    boolean exists(Long blockerId, Long blockedId);

    /** 양방향 차단 존재 여부. */
    boolean existsBetween(Long memberId, Long otherMemberId);

    List<BlockedMember> findBlockedMembers(Long blockerId);
}
