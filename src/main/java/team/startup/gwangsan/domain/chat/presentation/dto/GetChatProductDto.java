package team.startup.gwangsan.domain.chat.presentation.dto;

import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;

import java.time.LocalDateTime;
import java.util.List;

public record GetChatProductDto(
        Long id,
        String title,
        List<GetImageResponse> images,
        /**
         * 거래 요청(TradeComplete) 생성 시각. <b>상품 생성 시각이 아니다.</b>
         *
         * <p>클라이언트가 이 값으로 거래 카드의 노출 여부와 대화 흐름상 위치를 정한다.
         * 활성 요청도 완료된 요청도 없으면 null 이며, 거래 상태 변경 이벤트의 같은 이름
         * 필드와 항상 값이 일치한다.
         */
        LocalDateTime createdAt,
        boolean isSeller,
        boolean isCompletable,
        boolean isCompleted,
        boolean isReserved,
        LocalDateTime reservationScheduledAt,
        String reservationPlaceName,
        String reservationAddress,
        Double reservationLatitude,
        Double reservationLongitude
)  {
}
