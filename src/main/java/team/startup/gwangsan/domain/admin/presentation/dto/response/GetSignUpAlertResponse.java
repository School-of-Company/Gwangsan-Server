package team.startup.gwangsan.domain.admin.presentation.dto.response;

import java.time.LocalDateTime;

public record GetSignUpAlertResponse(
        Long memberId,
        String nickname,
        String title,
        String placeName,
        String recommenderNickname,
        LocalDateTime createdAt
) {
}
