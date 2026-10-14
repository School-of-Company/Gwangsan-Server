package team.startup.gwangsan.domain.post.model;

import lombok.Getter;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.post.exception.ForbiddenProductException;
import team.startup.gwangsan.domain.post.exception.ObjectRequiredImageException;
import team.startup.gwangsan.domain.post.exception.ProductAlreadyReservationException;
import team.startup.gwangsan.domain.post.exception.ProductNotOngoingException;
import team.startup.gwangsan.domain.post.exception.ReservedProductDeletionException;

import java.util.List;

/**
 * 게시글(상품). 저장 모델은 adapter/persistence/post 가 소유하고 이 타입과 명시적으로 변환한다.
 */
@Getter
public class Product {

    private final Long id;
    private final Long authorId;
    private Type type;
    private Mode mode;
    private String title;
    private String description;
    private Integer gwangsan;
    private ProductStatus status;

    public Product(Long id, Long authorId, Type type, Mode mode, String title, String description,
                   Integer gwangsan, ProductStatus status) {
        this.id = id;
        this.authorId = authorId;
        this.type = type;
        this.mode = mode;
        this.title = title;
        this.description = description;
        this.gwangsan = gwangsan;
        this.status = status;
    }

    public static Product create(Long authorId, Type type, Mode mode, String title, String description,
                                 Integer gwangsan, List<Long> imageIds) {
        requireImageIfObjectGiver(type, mode, imageIds);
        return new Product(null, authorId, type, mode, title, description, gwangsan, ProductStatus.ONGOING);
    }

    /**
     * 기존 동작대로 수정하면 상태가 ONGOING 으로 돌아간다.
     */
    public void update(Long actorId, Type type, Mode mode, String title, String description,
                       Integer gwangsan, List<Long> imageIds) {
        requireAuthor(actorId);
        this.type = type;
        this.mode = mode;
        this.title = title;
        this.description = description;
        this.gwangsan = gwangsan;
        this.status = ProductStatus.ONGOING;
        requireImageIfObjectGiver(type, mode, imageIds);
    }

    /**
     * 거래·리뷰 이력이 게시글을 참조하므로 물리 삭제하지 않고 상태만 바꾼다.
     */
    public void delete(Long actorId) {
        requireAuthor(actorId);
        if (status == ProductStatus.RESERVATION) {
            throw new ReservedProductDeletionException();
        }
        this.status = ProductStatus.DELETED;
    }

    public void reserve(Long actorId) {
        requireAuthor(actorId);
        if (status == ProductStatus.RESERVATION) {
            throw new ProductAlreadyReservationException();
        }
        if (status != ProductStatus.ONGOING) {
            throw new ProductNotOngoingException();
        }
        this.status = ProductStatus.RESERVATION;
    }

    /** 예약 취소·거래 철회로 다시 거래 가능 상태가 된다. */
    public void reopen() {
        this.status = ProductStatus.ONGOING;
    }

    public void complete() {
        this.status = ProductStatus.COMPLETED;
    }

    /** 관리자 삭제. 작성자 삭제와 달리 작성자·예약 상태를 검사하지 않는다(기존 관리자 삭제와 같음). */
    public void deleteByAdmin() {
        this.status = ProductStatus.DELETED;
    }

    public boolean isAuthor(Long memberId) {
        return authorId.equals(memberId);
    }

    public boolean isCompleted() {
        return status == ProductStatus.COMPLETED;
    }

    public boolean isReserved() {
        return status == ProductStatus.RESERVATION;
    }

    private void requireAuthor(Long actorId) {
        if (!isAuthor(actorId)) {
            throw new ForbiddenProductException();
        }
    }

    private static void requireImageIfObjectGiver(Type type, Mode mode, List<Long> imageIds) {
        if (type == Type.OBJECT && mode == Mode.GIVER && (imageIds == null || imageIds.isEmpty())) {
            throw new ObjectRequiredImageException();
        }
    }
}
