package team.startup.gwangsan.application.notice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.notice.port.out.NoticePort;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReassignNoticeAuthorService 단위 테스트")
class ReassignNoticeAuthorServiceTest {

    @InjectMocks private ReassignNoticeAuthorService service;

    @Mock private NoticePort noticePort;

    @Test
    @DisplayName("공지 작성자를 dummy 회원으로 재지정한다")
    void it_reassigns_notice_author() {
        service.reassign(1L, 2L);

        verify(noticePort).reassignAuthor(1L, 2L);
    }
}
