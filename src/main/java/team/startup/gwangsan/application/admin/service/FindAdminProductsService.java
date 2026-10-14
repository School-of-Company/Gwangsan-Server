package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.admin.data.AdminProduct;
import team.startup.gwangsan.application.admin.data.AdminProductSearch;
import team.startup.gwangsan.application.admin.port.in.FindAdminProductsQuery;
import team.startup.gwangsan.application.admin.port.out.AdminProductPort;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindAdminProductsService implements FindAdminProductsQuery {

    private final AdminProductPort adminProductPort;

    @Override
    @Transactional(readOnly = true)
    public List<AdminProduct> query(AdminProductSearch search) {
        return adminProductPort.findProducts(search);
    }
}
