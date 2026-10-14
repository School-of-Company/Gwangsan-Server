package team.startup.gwangsan.application.chat.data;

import team.startup.gwangsan.domain.chat.model.ChatImage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅방 상품·거래 상태 사실값. 보는 사람에 따라 달라지는 값은 chat 이 조합한다.
 * 완료 가능 여부는 거래 규칙 소유자(trade)가 역할별로 계산해 준다.
 */
public record ChatTradeCard(
        Long productId,
        String title,
        List<ChatImage> images,
        Long ownerId,
        boolean deleted,
        boolean completed,
        boolean reserved,
        boolean completableBySeller,
        boolean completableByBuyer,
        LocalDateTime requestedAt,
        Reservation reservation,
        LocalDateTime completedAt
) {

    /** 진행 중 예약. 없으면 null. */
    public record Reservation(LocalDateTime scheduledAt, LocalDateTime reservedAt, String placeName,
                              String address, Double latitude, Double longitude) {
    }
}
