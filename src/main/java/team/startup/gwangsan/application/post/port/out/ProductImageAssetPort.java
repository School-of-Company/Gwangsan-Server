package team.startup.gwangsan.application.post.port.out;

import java.util.List;
import java.util.Set;

public interface ProductImageAssetPort {
    /** 모두 존재하지 않으면 예외. 저장소가 돌려준 순서의 ID 목록(중복 제거)을 돌려준다. */
    List<Long> requireExisting(List<Long> imageIds);

    /** 연결이 끊긴 이미지 정리를 커밋 후 요청한다. */
    void requestUnusedCleanup(Long productId, Set<Long> imageIds);
}
