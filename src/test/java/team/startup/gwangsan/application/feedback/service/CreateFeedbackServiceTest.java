package team.startup.gwangsan.application.feedback.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.feedback.data.CreateFeedbackCommand;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackImagePort;
import team.startup.gwangsan.application.feedback.port.out.SaveFeedbackPort;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.model.Feedback;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;
import team.startup.gwangsan.domain.notification.model.OsType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateFeedbackService 단위 테스트")
class CreateFeedbackServiceTest {

    private static final ActorContext ACTOR = new ActorContext(1L, ActorContext.Role.ROLE_USER);

    @Mock private SaveFeedbackPort saveFeedbackPort;
    @Mock private LoadFeedbackImagePort loadFeedbackImagePort;

    private CreateFeedbackService service() {
        return new CreateFeedbackService(saveFeedbackPort, loadFeedbackImagePort);
    }

    @Nested
    @DisplayName("execute() 는")
    class Describe_execute {

        @Test
        @DisplayName("RECEIVED 상태의 문의를 저장하고 존재하는 이미지만 연결한다")
        void it_saves_feedback_and_existing_images_only() {
            when(saveFeedbackPort.save(any())).thenReturn(10L);
            when(loadFeedbackImagePort.findExistingImageIds(List.of(1L, 2L, 3L))).thenReturn(List.of(1L, 2L));

            service().execute(new CreateFeedbackCommand(ACTOR, FeedbackType.BUG, "버그 내용", List.of(1L, 2L, 3L),
                    "1.2.2", OsType.IOS, "18.0", "iPhone 15"));

            ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
            verify(saveFeedbackPort).save(captor.capture());
            Feedback saved = captor.getValue();
            assertThat(saved.getMemberId()).isEqualTo(1L);
            assertThat(saved.getType()).isEqualTo(FeedbackType.BUG);
            assertThat(saved.getContent()).isEqualTo("버그 내용");
            assertThat(saved.getStatus()).isEqualTo(FeedbackStatus.RECEIVED);
            verify(saveFeedbackPort).saveImages(10L, List.of(1L, 2L));
        }

        @Test
        @DisplayName("이미지 ID가 비어 있으면 이미지 연결을 생략한다")
        void it_skips_images_when_empty() {
            when(saveFeedbackPort.save(any())).thenReturn(11L);

            service().execute(new CreateFeedbackCommand(ACTOR, FeedbackType.ETC, "내용", List.of(), null, null, null, null));

            verify(saveFeedbackPort, never()).saveImages(any(), any());
            verifyNoInteractions(loadFeedbackImagePort);
        }
    }
}
