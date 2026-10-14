package team.startup.gwangsan.adapter.integration.post;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.event.image.DeleteNotUsedImageEvent;
import team.startup.gwangsan.adapter.http.ai.AiModerationClient;
import team.startup.gwangsan.application.image.data.ImageOwnerType;
import team.startup.gwangsan.application.image.data.ImageSummary;
import team.startup.gwangsan.application.image.port.in.ImageQuery;
import team.startup.gwangsan.application.post.port.out.ContentModerationPort;
import team.startup.gwangsan.application.post.port.out.ProductImageAssetPort;

import java.util.List;
import java.util.Set;

/**
 * image 연결. 이미지 존재 확인은 image 공개 {@link ImageQuery}, 미사용 이미지 정리는 커밋 후 image listener 가 처리하는 이벤트,
 * 게시글 텍스트 검열은 공유 AI 서버 클라이언트(adapter/http/ai)를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class PostImageBridgeAdapter implements ProductImageAssetPort, ContentModerationPort {

    private final ImageQuery imageQuery;
    private final AiModerationClient aiModerationClient;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<Long> requireExisting(List<Long> imageIds) {
        // 하나라도 없으면 ImageNotFoundException. 반환 순서는 DB 조회 순서(기존 findByIdIn 과 같은 IN 조회).
        return imageQuery.getAllByIds(imageIds).stream().map(ImageSummary::imageId).toList();
    }

    @Override
    public void requestUnusedCleanup(Long productId, Set<Long> imageIds) {
        // DeleteNotUsedImageEventListener 가 AFTER_COMMIT 비동기로 연결·이미지를 지운다.
        eventPublisher.publishEvent(new DeleteNotUsedImageEvent(productId, imageIds, ImageOwnerType.PRODUCT));
    }

    @Override
    public boolean containsProfanity(String text) {
        return aiModerationClient.containsProfanity(text);
    }
}
