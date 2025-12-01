package team.startup.gwangsan.domain.chat.repository.projection;

public record UnreadCountDto(
        Long roomId,
        Long unreadCount
) {
}