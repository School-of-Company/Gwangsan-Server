package team.startup.gwangsan.application.notice.data;

import java.util.List;

public record NoticeSummaryResult(Long id, String title, String content, List<NoticeImageResult> images, boolean isMe) {
}
