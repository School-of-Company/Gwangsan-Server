package team.startup.gwangsan.application.feedback.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.feedback.data.ChangeFeedbackStatusCommand;
import team.startup.gwangsan.application.feedback.port.out.AdminPlaceLookupPort;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort.FeedbackAccess;
import team.startup.gwangsan.application.feedback.port.out.SaveFeedbackPort;
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
@DisplayName("ChangeFeedbackStatusService 단위 테스트")
class ChangeFeedbackStatusServiceTest {

    private static final ActorContext HEAD_ADMIN = new ActorContext(1L, ActorContext.Role.ROLE_HEAD_ADMIN);

    @Mock private LoadFeedbackPort loadFeedbackPort;
    @Mock private SaveFeedbackPort saveFeedbackPort;
    @Mock private AdminPlaceLookupPort adminPlaceLookupPort;

    private ChangeFeedbackStatusService service() {
        return new ChangeFeedbackStatusService(loadFeedbackPort, saveFeedbackPort,
                new FeedbackAdminAccessGuard(adminPlaceLookupPort));
    }

    @Nested
    @DisplayName("execute() 는")
    class Describe_execute {

        @Test
        @DisplayName("범위 안이면 상태를 바꿔 저장한다")
        void it_changes_status() {
            when(loadFeedbackPort.findAccess(5L)).thenReturn(Optional.of(new FeedbackAccess(FeedbackType.BUG, 4)));
            when(loadFeedbackPort.findById(5L)).thenReturn(Optional.of(Feedback.of(5L, 9L, FeedbackType.BUG, "내용",
                    null, OsType.IOS, null, null, FeedbackStatus.RECEIVED, null, null, null, null)));

            service().execute(new ChangeFeedbackStatusCommand(HEAD_ADMIN, 5L, FeedbackStatus.IN_PROGRESS));

            ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
            verify(saveFeedbackPort).changeStatus(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(FeedbackStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("문의가 없으면 NotFoundFeedbackException 을 던진다")
        void it_throws_when_missing() {
            when(loadFeedbackPort.findAccess(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().execute(new ChangeFeedbackStatusCommand(HEAD_ADMIN, 5L, FeedbackStatus.DONE)))
                    .isInstanceOf(NotFoundFeedbackException.class);
            verifyNoInteractions(saveFeedbackPort);
        }
    }
}
