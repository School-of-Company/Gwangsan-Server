package team.startup.gwangsan.application.feedback.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import team.startup.gwangsan.application.feedback.data.*;
import team.startup.gwangsan.application.feedback.port.out.*;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.*;
import team.startup.gwangsan.domain.feedback.exception.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("문의 서비스 권한과 입력 검증")
class FeedbackServiceTest {
    private final FeedbackPort port = mock(FeedbackPort.class);
    private final FeedbackReferencesPort references = mock(FeedbackReferencesPort.class);
    private final FeedbackService service = new FeedbackService(port, references);
    private final ActorContext placeAdmin = new ActorContext(2L, ActorContext.Role.ROLE_PLACE_ADMIN);
    private final ActorContext headAdmin = new ActorContext(3L, ActorContext.Role.ROLE_HEAD_ADMIN);

    @BeforeEach
    void setUp() {
        lenient().when(references.requirePlace(2L)).thenReturn(10);
    }

    @Nested
    @DisplayName("지점 권한은")
    class Scope {
        @ParameterizedTest
        @EnumSource(FeedbackType.class)
        void it_allows_own_place(FeedbackType type) {
            when(port.find(1L)).thenReturn(Optional.of(detail(type, 10)));
            assertThat(service.detail(placeAdmin, 1L).feedback().type()).isEqualTo(type);
        }

        @ParameterizedTest
        @EnumSource(FeedbackType.class)
        void it_allows_head_admin_everywhere(FeedbackType type) {
            when(port.find(1L)).thenReturn(Optional.of(detail(type, 20)));
            service.detail(headAdmin, 1L);
            verify(references, never()).requirePlace(any());
        }

        @ParameterizedTest
        @EnumSource(value = FeedbackType.class, names = {"SUGGESTION", "TRADE", "ACCOUNT", "ETC"})
        void it_denies_other_place_for_all_operations(FeedbackType type) {
            when(port.find(1L)).thenReturn(Optional.of(detail(type, 20)));
            assertThatThrownBy(() -> service.detail(placeAdmin, 1L)).isInstanceOf(ForbiddenFeedbackException.class);
            assertThatThrownBy(() -> service.answer(placeAdmin, 1L, "답변")).isInstanceOf(ForbiddenFeedbackException.class);
            assertThatThrownBy(() -> service.changeStatus(placeAdmin, 1L, FeedbackStatus.DONE))
                    .isInstanceOf(ForbiddenFeedbackException.class);
            verify(port, never()).update(any());
            verify(references, never()).notifyAnswered(any(), any(), any());
        }

        @Test
        void it_allows_other_place_bug_and_records_answer_metadata() {
            when(port.find(1L)).thenReturn(Optional.of(detail(FeedbackType.BUG, 20)));
            service.answer(placeAdmin, 1L, "답변");
            verify(port).update(argThat(f -> f.status() == FeedbackStatus.DONE && f.answeredBy().equals(2L)
                    && f.answeredAt() != null && f.answer().equals("답변")));
            verify(references).notifyAnswered(1L, 1L, 2L);
        }

        @Test
        void it_ignores_requested_place_for_place_admin() {
            service.list(placeAdmin, FeedbackStatus.RECEIVED, FeedbackType.BUG, 20, 9L, 10);
            verify(port).search(new FeedbackSearch(null, 10, null, FeedbackStatus.RECEIVED, FeedbackType.BUG, 9L, 10));
        }

        @Test
        void it_filters_head_admin_by_requested_place() {
            service.list(headAdmin, null, null, 20, null, 20);
            verify(port).search(new FeedbackSearch(null, null, 20, null, null, null, 20));
        }

        @Test
        void it_rejects_regular_user_and_missing_feedback() {
            assertThatThrownBy(() -> service.detail(new ActorContext(1L, ActorContext.Role.ROLE_USER), 1L))
                    .isInstanceOf(ForbiddenFeedbackException.class);
            assertThatThrownBy(() -> service.detail(headAdmin, 99L)).isInstanceOf(NotFoundFeedbackException.class);
        }
    }

    @Nested
    @DisplayName("작성과 수정은")
    class Mutation {
        @Test
        void it_creates_received_with_validated_references() {
            new CreateFeedbackService(port, references).execute(command("내용", List.of(4L, 4L)));
            verify(references).requireMember(1L);
            verify(references).requireImages(List.of(4L));
            verify(port).create(argThat(f -> f.memberId().equals(1L) && f.status() == FeedbackStatus.RECEIVED),
                    eq(List.of(4L)));
        }

        @Test
        void it_rejects_bad_content_images_and_pagination() {
            assertThatThrownBy(() -> command(" ", List.of())).isInstanceOf(InvalidFeedbackException.class);
            assertThatThrownBy(() -> command("가".repeat(1001), List.of())).isInstanceOf(InvalidFeedbackException.class);
            assertThatThrownBy(() -> command("내용", List.of(0L))).isInstanceOf(InvalidFeedbackException.class);
            assertThatThrownBy(() -> command("내용", List.of(1L, 2L, 3L, 4L, 5L, 6L)))
                    .isInstanceOf(InvalidFeedbackException.class);
            assertThatThrownBy(() -> service.mine(1L, null, 0)).isInstanceOf(InvalidFeedbackException.class);
            assertThatThrownBy(() -> service.mine(1L, null, 101)).isInstanceOf(InvalidFeedbackException.class);
            assertThatThrownBy(() -> service.mine(1L, -1L, 10)).isInstanceOf(InvalidFeedbackException.class);
        }

        @Test
        void it_rejects_received_status_and_blank_answer() {
            when(port.find(1L)).thenReturn(Optional.of(detail(FeedbackType.BUG, 10)));
            assertThatThrownBy(() -> service.changeStatus(headAdmin, 1L, FeedbackStatus.RECEIVED))
                    .isInstanceOf(InvalidFeedbackException.class);
            assertThatThrownBy(() -> service.answer(headAdmin, 1L, " ")).isInstanceOf(InvalidFeedbackException.class);
            verify(port, never()).update(any());
        }

        @Test
        void it_preserves_answer_when_status_changes_and_does_not_notify() {
            Feedback answered = detail(FeedbackType.BUG, 10).feedback().answer("기존", 3L, LocalDateTime.now());
            when(port.find(1L)).thenReturn(Optional.of(new FeedbackDetail(answered, "작성자", 10, "지점", "관리자", List.of())));
            service.changeStatus(headAdmin, 1L, FeedbackStatus.IN_PROGRESS);
            verify(port).update(argThat(f -> f.status() == FeedbackStatus.IN_PROGRESS && f.answer().equals("기존")
                    && f.answeredBy().equals(3L)));
            verify(references, never()).notifyAnswered(any(), any(), any());
        }
    }

    private CreateFeedbackCommand command(String content, List<Long> images) {
        return new CreateFeedbackCommand(1L, FeedbackType.BUG, content, images, null, OsType.IOS, null, null);
    }

    private FeedbackDetail detail(FeedbackType type, Integer place) {
        LocalDateTime now = LocalDateTime.now();
        return new FeedbackDetail(new Feedback(1L, 1L, type, "문의", null, OsType.IOS, null, null,
                FeedbackStatus.RECEIVED, null, null, null, now, now), "작성자", place, "지점", null, List.of());
    }
}
