package team.startup.gwangsan.domain.notice.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.common.ActorContext.Role;
import team.startup.gwangsan.domain.notice.exception.NoticeForbiddenException;
import team.startup.gwangsan.domain.notice.exception.UnauthorizedNoticeAccessException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Notice 도메인 규칙")
class NoticeTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 1, 9, 0);
    private final Notice notice = new Notice(1L, "제목", "내용", 3, 10L, CREATED_AT);

    @Nested
    @DisplayName("updateBy() 는")
    class Describe_updateBy {

        @Test
        @DisplayName("작성자면 제목과 내용만 바꾼 공지를 돌려준다")
        void it_updates_title_and_content_for_author() {
            Notice updated = notice.updateBy(new ActorContext(10L, Role.ROLE_PLACE_ADMIN), "새 제목", "새 내용");

            assertThat(updated).isEqualTo(new Notice(1L, "새 제목", "새 내용", 3, 10L, CREATED_AT));
        }

        @Test
        @DisplayName("HEAD_ADMIN 이라도 작성자가 아니면 UnauthorizedNoticeAccessException 을 던진다")
        void it_throws_unauthorized_notice_access_exception_for_head_admin() {
            assertThatThrownBy(() -> notice.updateBy(new ActorContext(99L, Role.ROLE_HEAD_ADMIN), "t", "c"))
                    .isInstanceOf(UnauthorizedNoticeAccessException.class);
        }

        @Test
        @DisplayName("작성자가 없는 공지는 아무도 수정할 수 없다")
        void it_throws_unauthorized_notice_access_exception_without_author() {
            Notice orphan = new Notice(1L, "제목", "내용", 3, null, CREATED_AT);

            assertThatThrownBy(() -> orphan.updateBy(new ActorContext(10L, Role.ROLE_PLACE_ADMIN), "t", "c"))
                    .isInstanceOf(UnauthorizedNoticeAccessException.class);
        }
    }

    @Nested
    @DisplayName("checkDeletableBy() 는")
    class Describe_checkDeletableBy {

        @Test
        @DisplayName("작성자와 HEAD_ADMIN 을 허용한다")
        void it_allows_author_and_head_admin() {
            assertThatCode(() -> notice.checkDeletableBy(new ActorContext(10L, Role.ROLE_PLACE_ADMIN))).doesNotThrowAnyException();
            assertThatCode(() -> notice.checkDeletableBy(new ActorContext(99L, Role.ROLE_HEAD_ADMIN))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("작성자도 HEAD_ADMIN 도 아니면 NoticeForbiddenException 을 던진다")
        void it_throws_notice_forbidden_exception() {
            assertThatThrownBy(() -> notice.checkDeletableBy(new ActorContext(99L, Role.ROLE_PLACE_ADMIN)))
                    .isInstanceOf(NoticeForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("NoticeVisibility.canRead() 는")
    class Describe_visibility {

        @Test
        @DisplayName("일반 회원은 같은 지점 공지만 읽는다")
        void it_allows_same_place_only_for_non_head_admin() {
            assertThat(NoticeVisibility.canRead(Role.ROLE_PLACE_ADMIN, 3, 1, 3, 1)).isTrue();
            assertThat(NoticeVisibility.canRead(Role.ROLE_USER, 3, 1, 4, 1)).isFalse();
        }

        @Test
        @DisplayName("HEAD_ADMIN 은 같은 본부 산하 공지를 읽는다")
        void it_allows_same_head_for_head_admin() {
            assertThat(NoticeVisibility.canRead(Role.ROLE_HEAD_ADMIN, 3, 1, 4, 1)).isTrue();
            assertThat(NoticeVisibility.canRead(Role.ROLE_HEAD_ADMIN, 3, 1, 3, 2)).isFalse();
        }

        @Test
        @DisplayName("본부가 없는 HEAD_ADMIN 은 읽을 수 없다")
        void it_denies_head_admin_without_head() {
            assertThat(NoticeVisibility.canRead(Role.ROLE_HEAD_ADMIN, 3, null, 3, null)).isFalse();
        }
    }
}
