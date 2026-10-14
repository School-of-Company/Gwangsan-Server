package team.startup.gwangsan.application.post.port.in;

import team.startup.gwangsan.application.post.data.PendingReservationView;

import java.util.Optional;

/** 거래·채팅이 쓰는 진행 중 예약 공개 계약. 호출자 트랜잭션에 참여한다. */
public interface PendingReservationUseCase {

    Optional<PendingReservationView> findPending(Long productId);

    /**
     * 게시글의 PENDING 예약자가 구매자·판매자 중 한 명인지 확인하고 예약 ID 를 돌려준다.
     * 예약이 없거나 당사자가 아니면 ReservationParticipantOnlyException.
     */
    Long requireParticipant(Long productId, Long buyerId, Long sellerId);

    /** 예약을 COMPLETED 로 바꾼다. 없으면 ReservationParticipantOnlyException. */
    void complete(Long reservationId);
}
