package team.startup.gwangsan.adapter.integration.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.image.data.ImageOwnerType;
import team.startup.gwangsan.application.image.port.out.ImageReferenceCleanupPort;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;

// bridge: 링크 행은 각 업무 소유. 제거는 post=PR06, report=PR09, notice=PR10 (각 업무 공개 command 로 교체).
@Component
@RequiredArgsConstructor
public class LegacyImageReferenceCleanupAdapter implements ImageReferenceCleanupPort {

    private final ProductImageRepository productImageRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final ReportImageRepository reportImageRepository;

    @Override
    public void unlink(ImageOwnerType ownerType, Long sourceId, Long imageId) {
        switch (ownerType) {
            case PRODUCT -> productImageRepository.deleteByProductIdAndImageId(sourceId, imageId);
            case NOTICE -> noticeImageRepository.deleteByNoticeIdAndImageId(sourceId, imageId);
            case REPORT -> reportImageRepository.deleteByReportIdAndImageId(sourceId, imageId);
        }
    }
}
