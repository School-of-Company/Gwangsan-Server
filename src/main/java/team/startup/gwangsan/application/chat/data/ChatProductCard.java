package team.startup.gwangsan.application.chat.data;

import team.startup.gwangsan.domain.chat.model.ChatImage;

import java.time.LocalDateTime;
import java.util.List;

/** 채팅방 상단 상품·거래 카드. createdAt 은 거래 요청 시각이다 (상품 생성 시각 아님). */
public record ChatProductCard(
        Long id,
        String title,
        List<ChatImage> images,
        LocalDateTime createdAt,
        boolean isSeller,
        boolean isAuthor,
        boolean isCompletable,
        boolean isCompleted,
        boolean isReserved,
        boolean isDeleted,
        LocalDateTime reservationScheduledAt,
        LocalDateTime reservedAt,
        LocalDateTime completedAt,
        String reservationPlaceName,
        String reservationAddress,
        Double reservationLatitude,
        Double reservationLongitude
) {
}
