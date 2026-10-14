package team.startup.gwangsan.application.review.port.out;

import team.startup.gwangsan.application.review.data.ReviewImage;

import java.util.List;
import java.util.Map;

/** 상품(post 소유). */
public interface ReviewProductPort {
    /** 삭제되지 않은 상품이 거래 완료 상태인지. 상품이 없으면 상품 소유자의 기존 오류. */
    boolean isCompleted(long productId);

    Map<Long, List<ReviewImage>> findImages(List<Long> productIds);

    List<ReviewImage> findImages(long productId);
}
