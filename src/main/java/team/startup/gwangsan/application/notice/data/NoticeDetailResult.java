package team.startup.gwangsan.application.notice.data;

import java.time.LocalDateTime;
import java.util.List;

// viewerRole 은 작성자가 아니라 조회한 회원의 역할이다 (기존 응답 계약).
public record NoticeDetailResult(
        Long id,
        String title,
        String content,
        String placeName,
        LocalDateTime createdAt,
        String viewerRole,
        List<NoticeImageResult> images,
        boolean isMe
) {
}
