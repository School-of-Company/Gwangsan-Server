package team.startup.gwangsan.application.trade.port.out;

/** 예약(post 소유). */
public interface TradeReservationPort {
    /** 예약 중 상품의 대기 예약이 이 구매자·판매자 것인지 확인하고 예약 ID 를 돌려준다. 아니면 예약 소유자의 기존 오류. */
    long requireParticipantReservation(long productId, long buyerId, long sellerId);

    void completeReservation(long reservationId);
}
