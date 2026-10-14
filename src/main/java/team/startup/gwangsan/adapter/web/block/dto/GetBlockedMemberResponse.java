package team.startup.gwangsan.adapter.web.block.dto;

import team.startup.gwangsan.application.block.data.BlockedMember;

public record GetBlockedMemberResponse(
        Long memberId,
        String nickname
) {

    public static GetBlockedMemberResponse from(BlockedMember blockedMember) {
        return new GetBlockedMemberResponse(blockedMember.memberId(), blockedMember.nickname());
    }
}
