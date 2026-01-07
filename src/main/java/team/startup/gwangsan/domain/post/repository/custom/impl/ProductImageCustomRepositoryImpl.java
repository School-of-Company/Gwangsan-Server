package team.startup.gwangsan.domain.post.repository.custom.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.repository.custom.ProductImageCustomRepository;

import java.util.Collection;
import java.util.List;

import static team.startup.gwangsan.domain.post.entity.QProductImage.productImage;

@Repository
@RequiredArgsConstructor
public class ProductImageCustomRepositoryImpl implements ProductImageCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ProductImage> findAllByProductIdIn(Collection<Long> productIds) {
        return queryFactory
                .selectFrom(productImage)
                .join(productImage.image).fetchJoin()
                .join(productImage.product).fetchJoin()
                .where(productImage.product.id.in(productIds))
                .fetch();
    }

    @Override
    public List<ProductImage> findAllByProductId(Long productId) {
        return queryFactory
                .selectFrom(productImage)
                .join(productImage.image).fetchJoin()
                .join(productImage.product).fetchJoin()
                .where(productImage.product.id.eq(productId))
                .fetch();
    }
}
