package team.startup.gwangsan.application.trade.port.in;

import team.startup.gwangsan.application.trade.data.TradeCancelImageView;
import team.startup.gwangsan.application.trade.data.TradeCancelView;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 철회 요청 공개 조회. */
public interface TradeCancelQuery {
    /** 철회 요청 ID → 대상 상품 ID. 없는 ID 는 결과에서 빠진다. 빈 입력이면 조회하지 않는다. */
    Map<Long, Long> findProductIds(Collection<Long> tradeCancelIds);

    /** 철회 요청 목록(1 쿼리, 거래·상품 fetch join). 없는 ID 는 빠지고 순서는 저장소 결과 순서다. 빈 입력이면 조회하지 않는다. */
    List<TradeCancelView> findAll(Collection<Long> tradeCancelIds);

    /** 철회 요청별 첨부 이미지(1 쿼리, 이미지 fetch join). 이미지 없는 요청은 key 가 없다. 빈 입력이면 조회하지 않는다. */
    Map<Long, List<TradeCancelImageView>> findImages(Collection<Long> tradeCancelIds);

    /** 철회 요청자 회원 ID(1 쿼리, 요청자 fetch join). */
    Optional<Long> findRequesterId(long tradeCancelId);
}
