package team.startup.gwangsan.adapter.persistence.chat;

public record ChatRoomDto(
        Long roomId,
        Long opponentId,
        String opponentNickname,
        Long productId
) {
}
