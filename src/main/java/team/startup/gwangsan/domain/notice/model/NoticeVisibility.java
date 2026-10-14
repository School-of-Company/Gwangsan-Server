package team.startup.gwangsan.domain.notice.model;

import team.startup.gwangsan.domain.common.ActorContext;

import java.util.Objects;

/**
 * 공지 열람 범위. HEAD_ADMIN 은 같은 본부(head) 산하 지점의 공지를, 그 외는 자기 지점 공지만 본다.
 */
public final class NoticeVisibility {

    private NoticeVisibility() {
    }

    public static boolean canRead(ActorContext.Role readerRole,
                                  Integer readerPlaceId, Integer readerHeadId,
                                  Integer noticePlaceId, Integer noticeHeadId) {
        if (readerRole == ActorContext.Role.ROLE_HEAD_ADMIN) {
            // 기존 구현은 본부가 없는 HEAD_ADMIN 에서 NPE(500)였다. 열람 불가로 둔다.
            return readerHeadId != null && readerHeadId.equals(noticeHeadId);
        }
        return Objects.equals(readerPlaceId, noticePlaceId);
    }
}
