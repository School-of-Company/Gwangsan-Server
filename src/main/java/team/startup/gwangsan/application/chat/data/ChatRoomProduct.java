package team.startup.gwangsan.application.chat.data;

import team.startup.gwangsan.domain.chat.model.ChatImage;

import java.util.List;

public record ChatRoomProduct(
        Long productId,
        String title,
        boolean isCompleted,
        boolean isReserved,
        boolean isDeleted,
        List<ChatImage> images
) {

    /** 조회되지 않는 상품(삭제 등)의 목록 표시값. */
    public static ChatRoomProduct deleted(Long productId) {
        return new ChatRoomProduct(productId, null, false, false, true, List.of());
    }
}
