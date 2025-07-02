package team.startup.gwangsan.domain.post.repository.custom;

import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;

import java.util.List;

public interface ProductCustomRepository {
    List<Product> findProductsByTypeAndModeAndMemberDetailPlace(Type type, Mode mode, Place place);

    List<Product> findProductByMemberAndTypeAndMode(Member member, Type type, Mode mode);
}
