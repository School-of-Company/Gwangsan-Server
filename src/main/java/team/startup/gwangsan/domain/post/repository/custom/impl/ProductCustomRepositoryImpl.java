package team.startup.gwangsan.domain.post.repository.custom.impl;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomProductDto;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.repository.custom.ProductCustomRepository;
import com.querydsl.core.types.dsl.BooleanExpression;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static team.startup.gwangsan.domain.image.entity.QImage.image;
import static team.startup.gwangsan.domain.member.entity.QMember.member;
import static team.startup.gwangsan.domain.member.entity.QMemberDetail.memberDetail;
import static team.startup.gwangsan.domain.post.entity.QProduct.product;
import static team.startup.gwangsan.domain.post.entity.QProductImage.productImage;

@Repository
@RequiredArgsConstructor
public class ProductCustomRepositoryImpl implements ProductCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Product> findProductsByTypeAndModeAndMemberDetailPlaceAndStatus(Type type, Mode mode, Place place, ProductStatus status) {
        return queryFactory
                .selectFrom(product).distinct()
                .join(product.member, member).fetchJoin()
                .join(memberDetail).on(memberDetail.member.id.eq(member.id)).fetchJoin()
                .where(
                        typeEq(type),
                        modeEq(mode),
                        memberDetail.place.eq(place),
                        statusEq(status)
                )
                .fetch()
                .stream()
                .toList();
    }

    @Override
    public List<Product> findProductByMemberAndTypeAndModeAndStatus(Member member, Type type, Mode mode, ProductStatus status) {
        return queryFactory
                .selectFrom(product).distinct()
                .where(
                        product.member.id.eq(member.getId()),
                        typeEq(type),
                        modeEq(mode),
                        statusEq(status)
                )
                .fetch()
                .stream()
                .toList();
    }

    @Override
    public List<GetRoomProductDto> findRoomProductsWithImagesByIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        List<Tuple> rows = queryFactory
                .select(
                        product.id,
                        product.title,
                        image.id,
                        image.imageUrl
                )
                .from(productImage)
                .join(productImage.product, product)
                .join(productImage.image, image)
                .where(product.id.in(productIds))
                .fetch();

        Map<Long, GetRoomProductDto> resultMap = new LinkedHashMap<>();

        for (Tuple row : rows) {
            Long productId = row.get(product.id);
            String title = row.get(product.title);
            Long imageId = row.get(image.id);
            String imageUrl = row.get(image.imageUrl);

            GetRoomProductDto dto = resultMap.computeIfAbsent(
                    productId,
                    id -> new GetRoomProductDto(id, title, new ArrayList<>())
            );

            dto.images().add(new GetImageResponse(imageId, imageUrl));
        }

        return new ArrayList<>(resultMap.values());
    }

    private BooleanExpression typeEq(Type type) {
        return type != null ? product.type.eq(type) : null;
    }

    private BooleanExpression modeEq(Mode mode) {
        return mode != null ? product.mode.eq(mode) : null;
    }

    private BooleanExpression statusEq(ProductStatus status) {
        return status != null ? product.status.eq(status) : null;
    }
}
