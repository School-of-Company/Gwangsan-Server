package team.startup.gwangsan.application.trade.port.out;

import team.startup.gwangsan.application.trade.data.TradeImage;
import team.startup.gwangsan.domain.trade.model.TradeProduct;

import java.util.List;
import java.util.Map;

/** 상품(post 소유) 잠금·상태 변경. 상품이 없으면 구현체가 상품 소유자의 기존 오류를 던진다. */
public interface TradeProductPort {
    /** 삭제되지 않은 상품을 비관적 쓰기 잠금으로 읽는다. */
    TradeProduct lockActive(long productId);

    /** 삭제된 상품도 포함해 같은 행을 잠근다. 철회 대기 중 글이 지워져도 환불이 막히면 안 된다. */
    TradeProduct lockIncludingDeleted(long productId);

    /** 같은 트랜잭션에서 이미 바뀐 상태를 포함해 읽는다. */
    TradeProduct find(long productId);

    void complete(long productId);

    void reopen(long productId);

    /** 상품별 대표 이미지(조회 순서상 첫 장). 이미지가 없는 상품은 키가 없다. */
    Map<Long, TradeImage> findRepresentativeImages(List<Long> productIds);
}
