package team.startup.gwangsan.application.admin.port.in;

import team.startup.gwangsan.application.admin.data.AdminProduct;
import team.startup.gwangsan.application.admin.data.AdminProductSearch;

import java.util.List;

public interface FindAdminProductsQuery {
    List<AdminProduct> query(AdminProductSearch search);
}
