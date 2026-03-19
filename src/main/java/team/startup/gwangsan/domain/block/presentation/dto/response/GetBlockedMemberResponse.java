package team.startup.gwangsan.domain.block.presentation.dto.response;

public record GetBlockedMemberResponse(
        Long memberId,
        String nickname
) {
}
