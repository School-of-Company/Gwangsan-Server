package team.startup.gwangsan.adapter.persistence.chat;

public record UnreadCountDto(
        Long roomId,
        Long unreadCount
) {
}