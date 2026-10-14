package team.startup.gwangsan.application.post.port.in;

import team.startup.gwangsan.application.post.data.ProductSnapshot;

/**
 * 게시글 잠금·상태 변경 공개 command(trade·admin 소비).
 * 호출자의 트랜잭션에 참여(REQUIRED)하므로 잠금은 호출자 트랜잭션이 끝날 때 풀린다.
 * 없는 게시글은 NotFoundProductException(404).
 */
public interface ProductStatusUseCase {

    /** 삭제되지 않은 게시글을 PESSIMISTIC_WRITE 로 잠근다(거래 완료 요청·예약·작성자 삭제와 같은 행 잠금). */
    ProductSnapshot lockActive(Long productId);

    /** 삭제된 게시글도 잠근다(성립한 거래 철회·환불 경로). */
    ProductSnapshot lockIncludingDeleted(Long productId);

    /** 삭제된 게시글 포함, 상태를 COMPLETED 로 바꾼다. */
    void complete(Long productId);

    /** 삭제된 게시글 포함, 상태를 ONGOING 으로 바꾼다. */
    void reopen(Long productId);

    /** 관리자 삭제: 삭제되지 않은 게시글만 DELETED 로 바꾼다. 예약 상태를 검사하지 않는다. */
    void deleteByAdmin(Long productId);
}
