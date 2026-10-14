package team.startup.gwangsan.adapter.event.image;

import team.startup.gwangsan.application.image.data.ImageOwnerType;

import java.util.Set;

/**
 * 업무 커밋 후 미사용 이미지 정리를 요청하는 이벤트. 발행자는 post bridge(PostImageBridgeAdapter)이고
 * {@link DeleteNotUsedImageEventListener} 가 AFTER_COMMIT 비동기로 처리한다.
 */
public record DeleteNotUsedImageEvent(
        Long sourceId,
        Set<Long> imageIds,
        ImageOwnerType type
) {
}
