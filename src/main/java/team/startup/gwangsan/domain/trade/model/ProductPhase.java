package team.startup.gwangsan.domain.trade.model;

/**
 * 거래 규칙이 보는 상품 진행 단계. 상품 상태의 원본은 post 가 소유하고, 이 값은 그 사실의 사본이다.
 */
public enum ProductPhase {
    ONGOING,
    RESERVED,
    COMPLETED,
    DELETED
}
