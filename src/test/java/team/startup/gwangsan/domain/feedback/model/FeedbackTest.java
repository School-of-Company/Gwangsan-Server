package team.startup.gwangsan.domain.feedback.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.domain.notification.model.OsType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Feedback 도메인 모델")
class FeedbackTest {

    @Test
    @DisplayName("create() 는 상태를 RECEIVED 로, 답변 관련 필드를 비워 만든다")
    void it_creates_as_received() {
        Feedback feedback = Feedback.create(1L, FeedbackType.BUG, "내용", "1.0", OsType.ANDROID, "14", "모델");

        assertThat(feedback.getStatus()).isEqualTo(FeedbackStatus.RECEIVED);
        assertThat(feedback.getAnswer()).isNull();
        assertThat(feedback.getAnsweredByMemberId()).isNull();
        assertThat(feedback.getAnsweredAt()).isNull();
    }

    @Test
    @DisplayName("answer() 는 답변·답변자·답변 시각을 채우고 상태를 DONE 으로 바꾼다")
    void it_answers_and_marks_done() {
        Feedback feedback = Feedback.create(1L, FeedbackType.ETC, "내용", null, null, null, null);
        LocalDateTime now = LocalDateTime.now();

        feedback.answer("답변", 9L, now);

        assertThat(feedback.getAnswer()).isEqualTo("답변");
        assertThat(feedback.getAnsweredByMemberId()).isEqualTo(9L);
        assertThat(feedback.getAnsweredAt()).isEqualTo(now);
        assertThat(feedback.getStatus()).isEqualTo(FeedbackStatus.DONE);
    }

    @Test
    @DisplayName("changeStatus() 는 상태만 바꾼다")
    void it_changes_status_only() {
        Feedback feedback = Feedback.create(1L, FeedbackType.TRADE, "내용", null, null, null, null);

        feedback.changeStatus(FeedbackStatus.IN_PROGRESS);

        assertThat(feedback.getStatus()).isEqualTo(FeedbackStatus.IN_PROGRESS);
        assertThat(feedback.getAnswer()).isNull();
    }
}
