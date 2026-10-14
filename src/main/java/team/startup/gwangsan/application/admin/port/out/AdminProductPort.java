package team.startup.gwangsan.application.admin.port.out;

import team.startup.gwangsan.application.admin.data.AdminProduct;
import team.startup.gwangsan.application.admin.data.AdminProductSearch;

import java.util.List;

public interface AdminProductPort {

    List<AdminProduct> findProducts(AdminProductSearch search);

    /** 활성 상품만 DELETED 로 바꾼다. 없으면 NotFoundProduct. */
    void deleteActive(Long productId);
}
