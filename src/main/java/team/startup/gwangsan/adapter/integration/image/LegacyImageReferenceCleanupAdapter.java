package team.startup.gwangsan.adapter.integration.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.image.data.ImageOwnerType;
import team.startup.gwangsan.application.image.port.out.ImageReferenceCleanupPort;
import team.startup.gwangsan.application.notice.port.in.UnlinkNoticeImageUseCase;
import team.startup.gwangsan.application.post.port.in.UnlinkProductImageUseCase;
import team.startup.gwangsan.application.report.port.in.UnlinkReportImageUseCase;

// bridge: 링크 행은 각 업무 소유. post 는 공개 UnlinkProductImageUseCase 로 교체됨(PR06). report 는 공개 UnlinkReportImageUseCase 로 교체됨(PR09). notice 는 공개 UnlinkNoticeImageUseCase 로 교체됨(PR10).
@Component
@RequiredArgsConstructor
public class LegacyImageReferenceCleanupAdapter implements ImageReferenceCleanupPort {

    private final UnlinkProductImageUseCase unlinkProductImageUseCase;
    private final UnlinkNoticeImageUseCase unlinkNoticeImageUseCase;
    private final UnlinkReportImageUseCase unlinkReportImageUseCase;

    @Override
    public void unlink(ImageOwnerType ownerType, Long sourceId, Long imageId) {
        switch (ownerType) {
            case PRODUCT -> unlinkProductImageUseCase.unlink(sourceId, imageId);
            case NOTICE -> unlinkNoticeImageUseCase.unlink(sourceId, imageId);
            case REPORT -> unlinkReportImageUseCase.unlink(sourceId, imageId);
        }
    }
}
