package team.startup.gwangsan.application.post.port.in;

import team.startup.gwangsan.application.post.data.ProductDetailView;

public interface ProductDetailQuery {
    ProductDetailView query(Long actorId, Long productId);
}
