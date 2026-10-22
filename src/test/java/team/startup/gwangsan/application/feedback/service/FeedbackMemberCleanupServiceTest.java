package team.startup.gwangsan.application.feedback.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.feedback.port.out.SaveFeedbackPort;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackMemberCleanupService 단위 테스트")
class FeedbackMemberCleanupServiceTest {

    @Mock private SaveFeedbackPort saveFeedbackPort;

    private FeedbackMemberCleanupService service() {
        return new FeedbackMemberCleanupService(saveFeedbackPort);
    }

    @Test
    @DisplayName("reassign() 은 작성자·답변자 참조 재지정을 그대로 위임한다")
    void it_delegates_reassign() {
        service().reassign(1L, 99L);

        verify(saveFeedbackPort).reassignMember(1L, 99L);
    }

    @Test
    @DisplayName("unlink() 은 이미지 연결 행 삭제를 그대로 위임한다")
    void it_delegates_unlink() {
        service().unlink(5L, 7L);

        verify(saveFeedbackPort).deleteImageLink(5L, 7L);
    }
}
