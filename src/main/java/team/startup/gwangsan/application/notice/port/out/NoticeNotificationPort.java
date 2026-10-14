package team.startup.gwangsan.application.notice.port.out;

import java.util.List;

public interface NoticeNotificationPort {

    // 공지 등록 푸시와 알림함 기록을 커밋 후 처리로 요청한다. 실패해도 공지 저장을 되돌리지 않는다.
    void notifyCreated(Long noticeId, List<Long> memberIds);
}
