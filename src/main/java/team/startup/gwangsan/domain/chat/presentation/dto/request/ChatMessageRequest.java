package team.startup.gwangsan.domain.chat.presentation.dto.request;

public record ChatMessageRequest(
        Long productId,
        Long roomId,
        String content
) {
}
