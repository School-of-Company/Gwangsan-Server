package team.startup.gwangsan.domain.notice.model;

import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.notice.exception.NoticeForbiddenException;
import team.startup.gwangsan.domain.notice.exception.UnauthorizedNoticeAccessException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 공지. 작성자(authorId)는 탈퇴 시 dummy 회원으로 재지정되며, 과거 데이터에는 null 일 수 있다.
 */
public record Notice(
        Long id,
        String title,
        String content,
        Integer placeId,
        Long authorId,
        LocalDateTime createdAt
) {

    public static Notice create(String title, String content, Integer placeId, Long authorId) {
        return new Notice(null, title, content, placeId, authorId, null);
    }

    public boolean isWrittenBy(Long memberId) {
        return Objects.equals(authorId, memberId);
    }

    // 수정은 작성자만 가능하다. HEAD_ADMIN 도 예외가 아니다.
    public Notice updateBy(ActorContext actor, String title, String content) {
        if (!isWrittenBy(actor.memberId())) {
            throw new UnauthorizedNoticeAccessException();
        }
        return new Notice(id, title, content, placeId, authorId, createdAt);
    }

    // 삭제는 작성자 또는 HEAD_ADMIN 이 가능하다.
    public void checkDeletableBy(ActorContext actor) {
        if (!isWrittenBy(actor.memberId()) && actor.role() != ActorContext.Role.ROLE_HEAD_ADMIN) {
            throw new NoticeForbiddenException();
        }
    }
}
