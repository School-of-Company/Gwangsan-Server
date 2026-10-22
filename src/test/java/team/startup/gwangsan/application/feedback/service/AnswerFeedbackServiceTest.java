package team.startup.gwangsan.application.feedback.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.feedback.data.AnswerFeedbackCommand;
import team.startup.gwangsan.application.feedback.port.out.AdminPlaceLookupPort;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort.FeedbackAccess;
import team.startup.gwangsan.application.feedback.port.out.RequestFeedbackAnswerAlertPort;
import team.startup.gwangsan.application.feedback.port.out.SaveFeedbackPort;
import team.startup.gwangsan.domain.admin.exception.AdminPlaceMismatchException;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.feedback.exception.NotFoundFeedbackException;
import team.startup.gwangsan.domain.feedback.model.Feedback;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;
import team.startup.gwangsan.domain.notification.model.OsType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerFeedbackService 단위 테스트")
class AnswerFeedbackServiceTest {

    private static final ActorContext HEAD_ADMIN = new ActorContext(1L, ActorContext.Role.ROLE_HEAD_ADMIN);
    private static final ActorContext PLACE_ADMIN = new ActorContext(2L, ActorContext.Role.ROLE_PLACE_ADMIN);

    @Mock private LoadFeedbackPort loadFeedbackPort;
    @Mock private SaveFeedbackPort saveFeedbackPort;
    @Mock private RequestFeedbackAnswerAlertPort requestFeedbackAnswerAlertPort;
    @Mock private AdminPlaceLookupPort adminPlaceLookupPort;

    private AnswerFeedbackService service() {
        return new AnswerFeedbackService(loadFeedbackPort, saveFeedbackPort, requestFeedbackAnswerAlertPort,
                new FeedbackAdminAccessGuard(adminPlaceLookupPort));
    }

    private Feedback existingFeedback() {
        return Feedback.of(5L, 9L, FeedbackType.SUGGESTION, "내용", "1.0", OsType.ANDROID, "14", "모델",
                FeedbackStatus.RECEIVED, null, null, null, null);
    }

    @Nested
    @DisplayName("execute() 는")
    class Describe_execute {

        @Test
        @DisplayName("범위 안이면 답변을 저장해 상태를 DONE 으로 바꾸고 작성자에게 답변 알림을 요청한다")
        void it_saves_answer_and_requests_alert() {
            when(loadFeedbackPort.findAccess(5L)).thenReturn(Optional.of(new FeedbackAccess(FeedbackType.SUGGESTION, 4)));
            when(loadFeedbackPort.findById(5L)).thenReturn(Optional.of(existingFeedback()));

            service().execute(new AnswerFeedbackCommand(HEAD_ADMIN, 5L, "답변 내용"));

            ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
            verify(saveFeedbackPort).saveAnswer(captor.capture());
            Feedback saved = captor.getValue();
            assertThat(saved.getAnswer()).isEqualTo("답변 내용");
            assertThat(saved.getAnsweredByMemberId()).isEqualTo(1L);
            assertThat(saved.getStatus()).isEqualTo(FeedbackStatus.DONE);
            verify(requestFeedbackAnswerAlertPort).requestAnswerAlert(5L, 9L);
        }

        @Test
        @DisplayName("문의가 없으면 NotFoundFeedbackException 을 던지고 저장하지 않는다")
        void it_throws_when_missing() {
            when(loadFeedbackPort.findAccess(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().execute(new AnswerFeedbackCommand(HEAD_ADMIN, 5L, "답변")))
                    .isInstanceOf(NotFoundFeedbackException.class);
            verifyNoInteractions(saveFeedbackPort, requestFeedbackAnswerAlertPort);
        }

        @Test
        @DisplayName("PLACE_ADMIN 범위 밖이면 AdminPlaceMismatchException 을 던지고 저장하지 않는다")
        void it_rejects_out_of_scope() {
            when(loadFeedbackPort.findAccess(5L)).thenReturn(Optional.of(new FeedbackAccess(FeedbackType.TRADE, 4)));
            when(adminPlaceLookupPort.ownPlaceId(2L)).thenReturn(9);

            assertThatThrownBy(() -> service().execute(new AnswerFeedbackCommand(PLACE_ADMIN, 5L, "답변")))
                    .isInstanceOf(AdminPlaceMismatchException.class);
            verify(loadFeedbackPort, never()).findById(any());
            verifyNoInteractions(saveFeedbackPort, requestFeedbackAnswerAlertPort);
        }
    }
}
