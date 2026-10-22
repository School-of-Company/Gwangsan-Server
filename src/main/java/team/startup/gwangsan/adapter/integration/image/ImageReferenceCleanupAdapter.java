package team.startup.gwangsan.adapter.integration.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.feedback.port.in.UnlinkFeedbackImageUseCase;
import team.startup.gwangsan.application.image.data.ImageOwnerType;
import team.startup.gwangsan.application.image.port.out.ImageReferenceCleanupPort;
import team.startup.gwangsan.application.notice.port.in.UnlinkNoticeImageUseCase;
import team.startup.gwangsan.application.post.port.in.UnlinkProductImageUseCase;
import team.startup.gwangsan.application.report.port.in.UnlinkReportImageUseCase;

// 링크 행은 각 업무 소유라 post·notice·report·feedback 공개 Unlink UseCase 로 정리한다.
@Component
@RequiredArgsConstructor
public class ImageReferenceCleanupAdapter implements ImageReferenceCleanupPort {

    private final UnlinkProductImageUseCase unlinkProductImageUseCase;
    private final UnlinkNoticeImageUseCase unlinkNoticeImageUseCase;
    private final UnlinkReportImageUseCase unlinkReportImageUseCase;
    private final UnlinkFeedbackImageUseCase unlinkFeedbackImageUseCase;

    @Override
    public void unlink(ImageOwnerType ownerType, Long sourceId, Long imageId) {
        switch (ownerType) {
            case PRODUCT -> unlinkProductImageUseCase.unlink(sourceId, imageId);
            case NOTICE -> unlinkNoticeImageUseCase.unlink(sourceId, imageId);
            case REPORT -> unlinkReportImageUseCase.unlink(sourceId, imageId);
            case FEEDBACK -> unlinkFeedbackImageUseCase.unlink(sourceId, imageId);
        }
    }
}
