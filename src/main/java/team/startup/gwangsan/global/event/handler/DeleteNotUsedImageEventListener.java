package team.startup.gwangsan.global.event.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.global.event.DeleteNotUsedImageEvent;

@Component
@RequiredArgsConstructor
public class DeleteNotUsedImageEventListener {

    private final ProductImageRepository productImageRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final ReportImageRepository reportImageRepository;
    private final ImageRepository imageRepository;

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeleteNotUsedImageEvent(DeleteNotUsedImageEvent event) {
        for (Long imageId : event.imageIds()) {
            switch (event.type()) {
                case REPORT -> {
                    reportImageRepository.deleteByReportIdAndImageId(event.sourceId(), imageId);
                    imageRepository.deleteById(imageId);
                }
                case NOTICE -> {
                    noticeImageRepository.deleteByNoticeIdAndImageId(event.sourceId(), imageId);
                    imageRepository.deleteById(imageId);
                }
                case PRODUCT -> {
                    productImageRepository.deleteByProductIdAndImageId(event.sourceId(), imageId);
                    imageRepository.deleteById(imageId);
                }
            }
        }
    }
}
