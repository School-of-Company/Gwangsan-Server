package team.startup.gwangsan.adapter.integration.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.feedback.port.in.UnlinkFeedbackImageUseCase;
import team.startup.gwangsan.application.image.data.ImageOwnerType;
import team.startup.gwangsan.application.notice.port.in.UnlinkNoticeImageUseCase;
import team.startup.gwangsan.application.post.port.in.UnlinkProductImageUseCase;
import team.startup.gwangsan.application.report.port.in.UnlinkReportImageUseCase;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageReferenceCleanupAdapter 단위 테스트")
class ImageReferenceCleanupAdapterTest {

    @InjectMocks private ImageReferenceCleanupAdapter adapter;
    @Mock private UnlinkProductImageUseCase unlinkProductImageUseCase;
    @Mock private UnlinkNoticeImageUseCase unlinkNoticeImageUseCase;
    @Mock private UnlinkReportImageUseCase unlinkReportImageUseCase;
    @Mock private UnlinkFeedbackImageUseCase unlinkFeedbackImageUseCase;

    @Test
    @DisplayName("REPORT 는 신고 이미지 링크만 지운다")
    void it_unlinks_report_image() {
        adapter.unlink(ImageOwnerType.REPORT, 10L, 20L);

        verify(unlinkReportImageUseCase).unlink(10L, 20L);
        verifyNoInteractions(unlinkProductImageUseCase, unlinkNoticeImageUseCase, unlinkFeedbackImageUseCase);
    }

    @Test
    @DisplayName("NOTICE 는 공지 이미지 링크만 지운다")
    void it_unlinks_notice_image() {
        adapter.unlink(ImageOwnerType.NOTICE, 10L, 20L);

        verify(unlinkNoticeImageUseCase).unlink(10L, 20L);
        verifyNoInteractions(unlinkProductImageUseCase, unlinkReportImageUseCase, unlinkFeedbackImageUseCase);
    }

    @Test
    @DisplayName("PRODUCT 는 상품 이미지 링크만 지운다")
    void it_unlinks_product_image() {
        adapter.unlink(ImageOwnerType.PRODUCT, 10L, 20L);

        verify(unlinkProductImageUseCase).unlink(10L, 20L);
        verifyNoInteractions(unlinkNoticeImageUseCase, unlinkReportImageUseCase, unlinkFeedbackImageUseCase);
    }

    @Test
    @DisplayName("FEEDBACK 은 문의 이미지 링크만 지운다")
    void it_unlinks_feedback_image() {
        adapter.unlink(ImageOwnerType.FEEDBACK, 10L, 20L);

        verify(unlinkFeedbackImageUseCase).unlink(10L, 20L);
        verifyNoInteractions(unlinkProductImageUseCase, unlinkNoticeImageUseCase, unlinkReportImageUseCase);
    }
}
