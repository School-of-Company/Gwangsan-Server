package team.startup.gwangsan.domain.chat.repository.projection;

public record ChatRoomDto(
        Long roomId,
        Long opponentId,
        String opponentNickname,
        Long productId
) {
}
