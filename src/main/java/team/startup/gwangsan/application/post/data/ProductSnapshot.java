package team.startup.gwangsan.application.post.data;

import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Product;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;

/** 다른 업무에 공개하는 게시글 읽기 전용 값. 저장 모델(JPA Entity)은 노출하지 않는다. */
public record ProductSnapshot(
        Long id,
        Long authorId,
        String title,
        String description,
        Integer gwangsan,
        Type type,
        Mode mode,
        ProductStatus status
) {
    public static ProductSnapshot from(Product product) {
        return new ProductSnapshot(product.getId(), product.getAuthorId(), product.getTitle(),
                product.getDescription(), product.getGwangsan(), product.getType(), product.getMode(),
                product.getStatus());
    }

    public boolean isGiver() {
        return mode == Mode.GIVER;
    }

    public boolean isCompleted() {
        return status == ProductStatus.COMPLETED;
    }

    public boolean isReserved() {
        return status == ProductStatus.RESERVATION;
    }

    public boolean isDeleted() {
        return status == ProductStatus.DELETED;
    }
}
