package team.startup.gwangsan.application.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.post.data.AuthorProfile;
import team.startup.gwangsan.application.post.data.ImageView;
import team.startup.gwangsan.application.post.data.MemberProductsCriteria;
import team.startup.gwangsan.application.post.data.ProductAuthor;
import team.startup.gwangsan.application.post.data.ProductView;
import team.startup.gwangsan.application.post.port.in.MemberProductsQuery;
import team.startup.gwangsan.application.post.port.in.PlaceProductsQuery;
import team.startup.gwangsan.application.post.port.out.ProductImagePort;
import team.startup.gwangsan.application.post.port.out.ProductMemberPort;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.post.model.Product;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ProductListQueryService implements PlaceProductsQuery, MemberProductsQuery {

    private static final List<ProductStatus> VISIBLE =
            List.of(ProductStatus.ONGOING, ProductStatus.COMPLETED, ProductStatus.RESERVATION);

    private final ProductPort productPort;
    private final ProductImagePort productImagePort;
    private final ProductMemberPort memberPort;

    @Override
    @Transactional(readOnly = true)
    public List<ProductView> query(Long actorId, Type type, Mode mode) {
        Integer placeId = memberPort.placeIdOf(actorId);
        List<Product> products = productPort.findOngoingInPlace(type, mode, placeId);
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Long, AuthorProfile> profiles = memberPort.profiles(
                products.stream().map(Product::getAuthorId).distinct().toList());
        return toViews(products, product -> ProductAuthor.of(profiles.get(product.getAuthorId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductView> query(MemberProductsCriteria criteria) {
        AuthorProfile profile = memberPort.profile(criteria.memberId());
        List<Product> products = criteria.size() == null
                ? productPort.findByAuthor(criteria.memberId(), criteria.type(), criteria.mode(), VISIBLE)
                : productPort.findAuthorPage(criteria.memberId(), criteria.type(), criteria.mode(),
                        criteria.lastId(), criteria.size(), criteria.completed());
        if (products.isEmpty()) {
            return List.of();
        }
        ProductAuthor author = ProductAuthor.of(profile);
        return toViews(products, product -> author);
    }

    private List<ProductView> toViews(List<Product> products, Function<Product, ProductAuthor> authorOf) {
        Map<Long, List<ImageView>> images = productImagePort.findImages(
                products.stream().map(Product::getId).toList());
        return products.stream()
                .map(product -> new ProductView(product.getId(), product.getTitle(), product.getDescription(),
                        product.getGwangsan(), product.getType(), product.getMode(), authorOf.apply(product),
                        images.getOrDefault(product.getId(), List.of()), product.isCompleted(), product.isReserved()))
                .toList();
    }
}
