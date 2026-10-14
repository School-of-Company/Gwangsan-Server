package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.admin.port.in.DeleteAdminProductUseCase;
import team.startup.gwangsan.application.admin.port.out.AdminProductPort;

@Service
@RequiredArgsConstructor
public class DeleteAdminProductService implements DeleteAdminProductUseCase {

    private final AdminProductPort adminProductPort;

    // 기준선: 관리자 상품 삭제는 지점 범위를 검사하지 않는다(SecurityConfig 의 관리자 역할만).
    @Override
    @Transactional
    public void execute(Long productId) {
        adminProductPort.deleteActive(productId);
    }
}
