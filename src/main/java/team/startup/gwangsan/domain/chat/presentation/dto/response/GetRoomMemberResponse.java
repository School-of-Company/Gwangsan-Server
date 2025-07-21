package team.startup.gwangsan.domain.chat.presentation.dto.response;

public record GetRoomMemberResponse(
        Long memberId,
        String nickname
) {
}
