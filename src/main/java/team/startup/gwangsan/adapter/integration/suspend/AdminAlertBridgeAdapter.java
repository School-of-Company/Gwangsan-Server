package team.startup.gwangsan.adapter.integration.suspend;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.suspend.port.out.LoadAdminAlertPort;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;

// bridge: admin(PR11) 공개 query 가 생기면 그 input Port 호출로 교체한다.
@Component
@RequiredArgsConstructor
public class AdminAlertBridgeAdapter implements LoadAdminAlertPort {

    private final AdminAlertRepository adminAlertRepository;

    @Override
    public boolean exists(Long adminAlertId) {
        return adminAlertRepository.existsById(adminAlertId);
    }
}
