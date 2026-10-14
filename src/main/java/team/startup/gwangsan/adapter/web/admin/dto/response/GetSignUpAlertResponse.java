package team.startup.gwangsan.adapter.web.admin.dto.response;

import java.time.LocalDateTime;

public record GetSignUpAlertResponse(
        Long id,
        Long memberId,
        String nickname,
        String title,
        String placeName,
        String recommenderNickname,
        LocalDateTime createdAt
) {
}
