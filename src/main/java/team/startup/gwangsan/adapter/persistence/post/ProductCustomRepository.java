package team.startup.gwangsan.adapter.persistence.post;

import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.Collection;
import java.util.List;

public interface ProductCustomRepository {
    List<Product> findMemberProducts(Long memberId, Type type, Mode mode, Long lastId, int size, Boolean completed);

    List<Product> findAdminProducts(Type type, Mode mode, Long lastId, int size);

    List<Product> findProductsByTypeAndModeAndMemberDetailPlaceAndStatus(Type type, Mode mode, Place place, ProductStatus status);

    List<Product> findProductByMemberAndTypeAndModeAndStatusIn(MemberJpaEntity member, Type type, Mode mode, Collection<ProductStatus> statuses);
}
