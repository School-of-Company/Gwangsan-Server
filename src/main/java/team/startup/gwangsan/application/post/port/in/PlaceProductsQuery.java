package team.startup.gwangsan.application.post.port.in;

import team.startup.gwangsan.application.post.data.ProductView;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.List;

public interface PlaceProductsQuery {
    /** 실행자 지점의 진행 중 게시글. */
    List<ProductView> query(Long actorId, Type type, Mode mode);
}
