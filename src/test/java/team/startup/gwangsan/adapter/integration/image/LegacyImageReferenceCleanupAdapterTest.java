package team.startup.gwangsan.adapter.integration.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.image.data.ImageOwnerType;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LegacyImageReferenceCleanupAdapter 단위 테스트")
class LegacyImageReferenceCleanupAdapterTest {

    @InjectMocks private LegacyImageReferenceCleanupAdapter adapter;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private NoticeImageRepository noticeImageRepository;
    @Mock private ReportImageRepository reportImageRepository;

    @Test
    @DisplayName("REPORT 는 신고 이미지 링크만 지운다")
    void it_unlinks_report_image() {
        adapter.unlink(ImageOwnerType.REPORT, 10L, 20L);

        verify(reportImageRepository).deleteByReportIdAndImageId(10L, 20L);
        verifyNoInteractions(productImageRepository, noticeImageRepository);
    }

    @Test
    @DisplayName("NOTICE 는 공지 이미지 링크만 지운다")
    void it_unlinks_notice_image() {
        adapter.unlink(ImageOwnerType.NOTICE, 10L, 20L);

        verify(noticeImageRepository).deleteByNoticeIdAndImageId(10L, 20L);
        verifyNoInteractions(productImageRepository, reportImageRepository);
    }

    @Test
    @DisplayName("PRODUCT 는 상품 이미지 링크만 지운다")
    void it_unlinks_product_image() {
        adapter.unlink(ImageOwnerType.PRODUCT, 10L, 20L);

        verify(productImageRepository).deleteByProductIdAndImageId(10L, 20L);
        verifyNoInteractions(noticeImageRepository, reportImageRepository);
    }
}
