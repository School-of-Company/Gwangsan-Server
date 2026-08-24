package team.startup.gwangsan.domain.post.repository.custom;

import team.startup.gwangsan.domain.chat.presentation.dto.GetRoomProductDto;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;

import java.util.Collection;
import java.util.List;

public interface ProductCustomRepository {
    List<Product> findProductsByTypeAndModeAndMemberDetailPlaceAndStatusIn(Type type, Mode mode, Place place, Collection<ProductStatus> statuses);

    List<Product> findProductByMemberAndTypeAndModeAndStatusIn(Member member, Type type, Mode mode, Collection<ProductStatus> statuses);

    List<GetRoomProductDto> findRoomProductsWithImagesByIds(Collection<Long> productIds);
}
