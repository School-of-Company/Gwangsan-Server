package team.startup.gwangsan.application.chat.data;

import java.util.List;

public record ChatRoomMessages(ChatProductCard product, List<ChatMessageView> messages) {
}
