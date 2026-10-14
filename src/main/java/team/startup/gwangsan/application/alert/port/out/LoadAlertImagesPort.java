package team.startup.gwangsan.application.alert.port.out;

import team.startup.gwangsan.application.alert.data.AlertImage;

import java.util.List;
import java.util.Map;

/**
 * 알림 원천별 이미지 조회. 각 맵의 key 는 공지/상품/신고 ID 다.
 */
public interface LoadAlertImagesPort {
    Map<Long, List<AlertImage>> noticeImages(List<Long> noticeIds);

    Map<Long, List<AlertImage>> productImages(List<Long> productIds);

    Map<Long, List<AlertImage>> reportImages(List<Long> reportIds);

    /** 거래 철회 ID → 상품 ID. 상품을 찾을 수 없는 철회는 빠진다. */
    Map<Long, Long> productIdsByTradeCancel(List<Long> tradeCancelIds);
}
