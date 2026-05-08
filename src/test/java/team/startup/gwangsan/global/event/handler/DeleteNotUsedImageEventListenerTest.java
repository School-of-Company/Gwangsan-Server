package team.startup.gwangsan.global.event.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.report.repository.ReportImageRepository;
import team.startup.gwangsan.global.event.DeleteNotUsedImageEvent;
import team.startup.gwangsan.global.event.constant.ImageType;

import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteNotUsedImageEventListener 단위 테스트")
class DeleteNotUsedImageEventListenerTest {

    @InjectMocks private DeleteNotUsedImageEventListener listener;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private NoticeImageRepository noticeImageRepository;
    @Mock private ReportImageRepository reportImageRepository;
    @Mock private ImageRepository imageRepository;

    @Nested
    @DisplayName("handleDeleteNotUsedImageEvent() 메서드는")
    class Describe_handleDeleteNotUsedImageEvent {

        @Nested
        @DisplayName("ImageType이 REPORT일 때")
        class Context_with_report_type {

            @Test
            @DisplayName("reportImageRepository와 imageRepository를 호출한다")
            void it_calls_report_and_image_repository() {
                Long sourceId = 10L;
                Long imageId = 20L;
                DeleteNotUsedImageEvent event = new DeleteNotUsedImageEvent(sourceId, Set.of(imageId), ImageType.REPORT);

                listener.handleDeleteNotUsedImageEvent(event);

                verify(reportImageRepository).deleteByReportIdAndImageId(sourceId, imageId);
                verify(imageRepository).deleteById(imageId);
            }
        }

        @Nested
        @DisplayName("ImageType이 NOTICE일 때")
        class Context_with_notice_type {

            @Test
            @DisplayName("noticeImageRepository와 imageRepository를 호출한다")
            void it_calls_notice_and_image_repository() {
                Long sourceId = 10L;
                Long imageId = 20L;
                DeleteNotUsedImageEvent event = new DeleteNotUsedImageEvent(sourceId, Set.of(imageId), ImageType.NOTICE);

                listener.handleDeleteNotUsedImageEvent(event);

                verify(noticeImageRepository).deleteByNoticeIdAndImageId(sourceId, imageId);
                verify(imageRepository).deleteById(imageId);
            }
        }

        @Nested
        @DisplayName("ImageType이 PRODUCT일 때")
        class Context_with_product_type {

            @Test
            @DisplayName("productImageRepository와 imageRepository를 호출한다")
            void it_calls_product_and_image_repository() {
                Long sourceId = 10L;
                Long imageId = 20L;
                DeleteNotUsedImageEvent event = new DeleteNotUsedImageEvent(sourceId, Set.of(imageId), ImageType.PRODUCT);

                listener.handleDeleteNotUsedImageEvent(event);

                verify(productImageRepository).deleteByProductIdAndImageId(sourceId, imageId);
                verify(imageRepository).deleteById(imageId);
            }
        }

        @Nested
        @DisplayName("imageIds가 비어있을 때")
        class Context_with_empty_image_ids {

            @Test
            @DisplayName("아무 레포지토리도 호출하지 않는다")
            void it_does_not_call_any_repository() {
                DeleteNotUsedImageEvent event = new DeleteNotUsedImageEvent(10L, Set.of(), ImageType.REPORT);

                listener.handleDeleteNotUsedImageEvent(event);

                verifyNoInteractions(reportImageRepository, noticeImageRepository, productImageRepository, imageRepository);
            }
        }
    }
}